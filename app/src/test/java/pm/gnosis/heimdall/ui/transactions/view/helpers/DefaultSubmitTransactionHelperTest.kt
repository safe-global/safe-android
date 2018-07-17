package pm.gnosis.heimdall.ui.transactions.view.helpers

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.*
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.helpers.SignatureStore
import pm.gnosis.heimdall.ui.transactions.view.TransactionInfoViewHolder
import pm.gnosis.heimdall.ui.transactions.view.viewholders.AssetTransferViewHolder
import pm.gnosis.heimdall.ui.transactions.view.viewholders.GenericTransactionViewHolder
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestSingleFactory
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexToByteArray
import java.math.BigInteger
import java.util.concurrent.TimeoutException

@RunWith(MockitoJUnitRunner::class)
class DefaultSubmitTransactionHelperTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var addressBookRepository: AddressBookRepository

    @Mock
    private lateinit var safeRepository: GnosisSafeRepository

    @Mock
    private lateinit var signaturePushRepository: PushServiceRepository

    @Mock
    private lateinit var signatureStore: SignatureStore

    @Mock
    private lateinit var transactionData: TransactionData

    @Mock
    private lateinit var transactionViewHolder: TransactionInfoViewHolder

    @Mock
    private lateinit var transactionViewHolderBuilder: TransactionViewHolderBuilder

    @Mock
    private lateinit var relayRepositoryMock: TransactionExecutionRepository

    private lateinit var addressHelper: AddressHelper

    private lateinit var submitTransactionHelper: SubmitTransactionHelper

    @Before
    fun setUp() {
        given(transactionViewHolder.loadTransaction())
            .willReturn(Single.just(TEST_TRANSACTION))
        given(transactionViewHolderBuilder.build(MockUtils.any(), MockUtils.any(), anyBoolean()))
            .willReturn(Single.just(transactionViewHolder))
        addressHelper = AddressHelper(addressBookRepository, safeRepository)
        submitTransactionHelper =
                DefaultSubmitTransactionHelper(relayRepositoryMock, signaturePushRepository, signatureStore, transactionViewHolderBuilder)
    }

    @Test
    fun testFlow() {
        val error = TimeoutException()
        val estimationSingleFactory = TestSingleFactory<TransactionExecutionRepository.ExecuteInformation>()
        given(relayRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any())).willReturn(estimationSingleFactory.get())
        val signatureSubject = PublishSubject.create<Map<Solidity.Address, Signature>>()
        given(signatureStore.flatMapInfo(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(signatureSubject)
        val signatureSingleFactory = TestSingleFactory<Map<Solidity.Address, Signature>>()
        given(signatureStore.load()).willReturn(signatureSingleFactory.get())
        val pushMessageSubject = PublishSubject.create<PushServiceRepository.TransactionResponse>()
        given(signaturePushRepository.observe(anyString())).willReturn(pushMessageSubject)
        val hashSingleFactory = TestSingleFactory<ByteArray>()
        given(
            relayRepositoryMock.calculateHash(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        )
            .will { hashSingleFactory.get() }

        submitTransactionHelper.setup(TEST_SAFE, ::loadExecutionInfo)
        val retryEvents = PublishSubject.create<Unit>()
        val requestEvents = PublishSubject.create<Unit>()
        val submitEvents = PublishSubject.create<Unit>()
        val events = SubmitTransactionHelper.Events(
            retryEvents, requestEvents, submitEvents
        )
        val testObserver = TestObserver<Result<SubmitTransactionHelper.ViewUpdate>>()

        submitTransactionHelper.observe(events, transactionData)
            .subscribe(testObserver)

        then(transactionViewHolderBuilder).should().build(TEST_SAFE, transactionData, true)
        then(transactionViewHolder).should().loadTransaction()

        val updates = mutableListOf<((Result<SubmitTransactionHelper.ViewUpdate>) -> Boolean)>({
            ((it as? DataResult)?.data as? SubmitTransactionHelper.ViewUpdate.TransactionInfo)?.viewHolder == transactionViewHolder
        })
        testObserver.assertUpdates(updates)

        // Estimate error
        estimationSingleFactory.error(error)
        updates += { it == ErrorResult<SubmitTransactionHelper.ViewUpdate>(error) }
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.EstimateError) }
        testObserver.assertUpdates(updates)

        // Estimate, balance too low
        val info = TransactionExecutionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH,
            TEST_TRANSACTION, TEST_OWNERS[2], TEST_OWNERS.size - 1,
            TEST_OWNERS, BigInteger.ONE, BigInteger.TEN, BigInteger.ZERO,
            Wei.ether("23")
        )
        given(relayRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(info))
        retryEvents.onNext(Unit)

        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.Estimate(Wei(BigInteger.valueOf(10)), Wei.ether("23"))) }
        testObserver.assertUpdates(updates)

        /*
         * Test ready state
         */
        signatureSubject.onNext(emptyMap())
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.Confirmations(false)) }
        testObserver.assertUpdates(updates)

        signatureSubject.onNext(mapOf(TEST_OWNERS[0] to TEST_SIGNATURE))
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.Confirmations(true)) }
        testObserver.assertUpdates(updates)

        then(signaturePushRepository).should().observe(TEST_TRANSACTION_HASH)
        /*
         * Test error requesting confirmation
         */
        given(
            signaturePushRepository.requestConfirmations(
                anyString(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                anySet()
            )
        )
            .willReturn(Completable.error(error))
        signatureSingleFactory.success(emptyMap())
        hashSingleFactory.success("7e4cb4cd190aedb510e8c4d366a87a8ee948921796ea7d720c74db3fc4be4db3".hexToByteArray())
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.ConfirmationsError) }
        updates += { it == ErrorResult<SubmitTransactionHelper.ViewUpdate>(error) }
        testObserver.assertUpdates(updates)
        then(signaturePushRepository).should()
            .requestConfirmations(
                "0x7e4cb4cd190aedb510e8c4d366a87a8ee948921796ea7d720c74db3fc4be4db3",
                TEST_SAFE,
                info.transaction,
                info.txGas,
                info.dataGas,
                info.gasPrice,
                setOf(TEST_OWNERS[0], TEST_OWNERS[1])
            )

        /*
         * Test success requesting confirmation
         */
        // No new events before actual request
        requestEvents.onNext(Unit)
        testObserver.assertUpdates(updates)

        given(
            signaturePushRepository.requestConfirmations(
                anyString(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                anySet()
            )
        )
            .willReturn(Completable.complete())
        signatureSingleFactory.success(mapOf(TEST_OWNERS[0] to TEST_SIGNATURE))
        hashSingleFactory.success("7e4cb4cd190aedb510e8c4d366a87a8ee948921796ea7d720c74db3fc4be4db3".hexToByteArray())
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.ConfirmationsRequested) }
        testObserver.assertUpdates(updates)
        then(signaturePushRepository).should()
            .requestConfirmations(
                "0x7e4cb4cd190aedb510e8c4d366a87a8ee948921796ea7d720c74db3fc4be4db3",
                TEST_SAFE,
                info.transaction,
                info.txGas,
                info.dataGas,
                info.gasPrice,
                setOf(TEST_OWNERS[1])
            )

        /*
         * Test submit transaction failure
         */
        given(
            relayRepositoryMock.submit(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                anyBoolean(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                anyBoolean()
            )
        ).willReturn(Single.error(error))
        submitEvents.onNext(Unit)
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.TransactionSubmitted(false)) }
        updates += { it == ErrorResult<SubmitTransactionHelper.ViewUpdate>(error) }
        testObserver.assertUpdates(updates)
        then(signaturePushRepository).shouldHaveNoMoreInteractions()

        /*
         * Test submit transaction push failure -> should still be returned as submitted
         */
        given(
            relayRepositoryMock.submit(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                anyBoolean(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                anyBoolean()
            )
        ).willReturn(Single.just(TEST_CHAIN_HASH))
        given(
            signaturePushRepository.propagateSubmittedTransaction(anyString(), anyString(), anySet())
        ).willReturn(Completable.error(TimeoutException()))
        submitEvents.onNext(Unit)
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.TransactionSubmitted(true)) }
        testObserver.assertUpdates(updates)
        then(signaturePushRepository).should()
            .propagateSubmittedTransaction(TEST_TRANSACTION_HASH, TEST_CHAIN_HASH, setOf(TEST_OWNERS[0], TEST_OWNERS[1]))

        /*
         * Test submit transaction success
         */
        given(
            relayRepositoryMock.submit(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                anyBoolean(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                anyBoolean()
            )
        ).willReturn(Single.just(TEST_CHAIN_HASH))
        given(
            signaturePushRepository.propagateSubmittedTransaction(anyString(), anyString(), anySet())
        ).willReturn(Completable.complete())
        submitEvents.onNext(Unit)
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.TransactionSubmitted(true)) }
        testObserver.assertUpdates(updates)
        then(signaturePushRepository).should(times(2))
            .propagateSubmittedTransaction(TEST_TRANSACTION_HASH, TEST_CHAIN_HASH, setOf(TEST_OWNERS[0], TEST_OWNERS[1]))

        /*
         * Test confirmation push message
         */
        given(
            relayRepositoryMock.checkConfirmation(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.just(TEST_OWNERS[0] to TEST_SIGNATURE))
        pushMessageSubject.onNext(PushServiceRepository.TransactionResponse.Confirmed(TEST_SIGNATURE))
        testObserver.assertUpdates(updates)

        /*
         * Test confirmation push message error
         */
        given(
            relayRepositoryMock.checkConfirmation(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.error(error))
        pushMessageSubject.onNext(PushServiceRepository.TransactionResponse.Confirmed(TEST_SIGNATURE))
        updates += { it == ErrorResult<SubmitTransactionHelper.ViewUpdate>(error) }
        testObserver.assertUpdates(updates)

        /*
         * Test reject push message wrong sender
         */
        given(
            relayRepositoryMock.checkRejection(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.just(TEST_ADDRESS to TEST_SIGNATURE))
        pushMessageSubject.onNext(PushServiceRepository.TransactionResponse.Rejected(TEST_SIGNATURE))
        testObserver.assertUpdates(updates)

        /*
         * Test reject push message error
         */
        given(
            relayRepositoryMock.checkRejection(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.error(error))
        pushMessageSubject.onNext(PushServiceRepository.TransactionResponse.Rejected(TEST_SIGNATURE))
        updates += { it == ErrorResult<SubmitTransactionHelper.ViewUpdate>(error) }
        testObserver.assertUpdates(updates)

        /*
         * Test reject push message
         */
        given(
            relayRepositoryMock.checkRejection(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.just(TEST_OWNERS[0] to TEST_SIGNATURE))
        pushMessageSubject.onNext(PushServiceRepository.TransactionResponse.Rejected(TEST_SIGNATURE))
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.TransactionRejected) }
        testObserver.assertUpdates(updates)
    }

    @Test
    fun singleOwner() {
        val signatureSubject = PublishSubject.create<Map<Solidity.Address, Signature>>()
        given(signatureStore.flatMapInfo(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(signatureSubject)
        val pushMessageSubject = PublishSubject.create<PushServiceRepository.TransactionResponse>()
        given(signaturePushRepository.observe(anyString())).willReturn(pushMessageSubject)

        submitTransactionHelper.setup(TEST_SAFE, ::loadExecutionInfo)
        val retryEvents = PublishSubject.create<Unit>()
        val requestEvents = PublishSubject.create<Unit>()
        val submitEvents = PublishSubject.create<Unit>()
        val events = SubmitTransactionHelper.Events(
            retryEvents, requestEvents, submitEvents
        )
        val testObserver = TestObserver<Result<SubmitTransactionHelper.ViewUpdate>>()

        val info = TransactionExecutionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH,
            TEST_TRANSACTION, TEST_OWNERS[0], 1,
            TEST_OWNERS.subList(0, 1), BigInteger.ONE, BigInteger.TEN, BigInteger.ZERO,
            Wei.ether("23")
        )
        given(relayRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(info))

        submitTransactionHelper.observe(events, TransactionData.AssetTransfer(TEST_ETHER_TOKEN, TEST_ETH_AMOUNT, TEST_ADDRESS))
            .subscribe(testObserver)

        val updates = mutableListOf<((Result<SubmitTransactionHelper.ViewUpdate>) -> Boolean)>({
            ((it as? DataResult)?.data as? SubmitTransactionHelper.ViewUpdate.TransactionInfo)?.viewHolder == transactionViewHolder
        })
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.Estimate(Wei(BigInteger.valueOf(10)), Wei.ether("23"))) }
        testObserver.assertUpdates(updates)

        signatureSubject.onNext(emptyMap())
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.Confirmations(true)) }
        testObserver.assertUpdates(updates)
    }

    private fun loadExecutionInfo(transaction: SafeTransaction) =
        relayRepositoryMock.loadExecuteInformation(TEST_SAFE, transaction)

    private fun TestObserver<Result<SubmitTransactionHelper.ViewUpdate>>.assertUpdates(updates: List<((Result<SubmitTransactionHelper.ViewUpdate>) -> Boolean)>): TestObserver<Result<SubmitTransactionHelper.ViewUpdate>> {
        System.out.println("Updated: ${values()}")
        assertValueCount(updates.size)
        updates.forEachIndexed { index, predicate ->
            assertValueAt(index, predicate)
        }
        assertNoErrors()
        assertNotComplete()
        return this
    }

    companion object {
        private val TEST_SAFE = "0xA7e15e2e76Ab469F8681b576cFF168F37Aa246EC".asEthereumAddress()!!
        private val TEST_ADDRESS = "0xc257274276a4e539741ca11b590b9447b26a8051".asEthereumAddress()!!
        private val TEST_ETHER_TOKEN = Solidity.Address(BigInteger.ZERO)
        private val TEST_ETH_AMOUNT = Wei.ether("23").value
        private const val TEST_CHAIN_HASH = "SomeChainHash"
        private const val TEST_TRANSACTION_HASH = "SomeHash"
        private val TEST_TRANSACTION =
            SafeTransaction(Transaction(Solidity.Address(BigInteger.ZERO), nonce = BigInteger.TEN), TransactionExecutionRepository.Operation.CALL)
        private val TEST_SIGNERS = listOf(BigInteger.valueOf(7), BigInteger.valueOf(13)).map { Solidity.Address(it) }
        private val TEST_OWNERS = TEST_SIGNERS + Solidity.Address(BigInteger.valueOf(5))
        private val TEST_SIGNATURE = Signature(BigInteger.TEN, BigInteger.TEN, 27)
    }
}
