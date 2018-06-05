package pm.gnosis.heimdall.ui.transactions.view.helpers

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.helpers.SignatureStore
import pm.gnosis.heimdall.ui.transactions.view.review.ReviewTransactionViewModel
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
    private lateinit var tokenRepositoryMock: TokenRepository

    @Mock
    private lateinit var relayRepositoryMock: TransactionExecutionRepository

    private lateinit var addressHelper: AddressHelper

    private lateinit var submitTransactionHelper: SubmitTransactionHelper

    @Before
    fun setUp() {
        addressHelper = AddressHelper(addressBookRepository, safeRepository)
        submitTransactionHelper =
                DefaultSubmitTransactionHelper(addressHelper, relayRepositoryMock, signaturePushRepository, signatureStore, tokenRepositoryMock)
    }

    private fun testFlow(data: TransactionData, viewHolderCheck: ((Result<SubmitTransactionHelper.ViewUpdate>) -> Boolean)) {
        val error = TimeoutException()
        val estimationSingleFactory = TestSingleFactory<TransactionExecutionRepository.ExecuteInformation>()
        BDDMockito.given(relayRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any())).willReturn(estimationSingleFactory.get())
        val signatureSubject = PublishSubject.create<Map<Solidity.Address, Signature>>()
        BDDMockito.given(signatureStore.flatMapInfo(MockUtils.any(), MockUtils.any())).willReturn(signatureSubject)
        val pushMessageSubject = PublishSubject.create<PushServiceRepository.TransactionResponse>()
        BDDMockito.given(signaturePushRepository.observe(BDDMockito.anyString())).willReturn(pushMessageSubject)
        val hashSingleFactory = TestSingleFactory<ByteArray>()
        BDDMockito.given(
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

        submitTransactionHelper.observe(events, data)
            .subscribe(testObserver)

        val updates = mutableListOf(viewHolderCheck)
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
        BDDMockito.given(relayRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(info))
        retryEvents.onNext(Unit)

        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.Estimate(Wei(BigInteger.valueOf(32010)), Wei.ether("23"))) }
        testObserver.assertUpdates(updates)

        /*
         * Test error requesting confirmation
         */
        BDDMockito.given(
            signaturePushRepository.requestConfirmations(
                BDDMockito.anyString(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                BDDMockito.anySet()
            )
        )
            .willReturn(Completable.error(error))
        hashSingleFactory.success("7e4cb4cd190aedb510e8c4d366a87a8ee948921796ea7d720c74db3fc4be4db3".hexToByteArray())
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.ConfirmationsError) }
        updates += { it == ErrorResult<SubmitTransactionHelper.ViewUpdate>(error) }
        testObserver.assertUpdates(updates)

        /*
         * Test success requesting confirmation
         */
        // No new events before actual request
        requestEvents.onNext(Unit)
        testObserver.assertUpdates(updates)

        BDDMockito.given(
            signaturePushRepository.requestConfirmations(
                BDDMockito.anyString(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                BDDMockito.anySet()
            )
        )
            .willReturn(Completable.complete())
        hashSingleFactory.success("7e4cb4cd190aedb510e8c4d366a87a8ee948921796ea7d720c74db3fc4be4db3".hexToByteArray())
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.ConfirmationsRequested) }
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

        /*
         * Test submit transaction failure
         */
        BDDMockito.given(
            relayRepositoryMock.submit(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                BDDMockito.anyBoolean(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Completable.error(error))
        submitEvents.onNext(Unit)
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.TransactionSubmitted(false)) }
        updates += { it == ErrorResult<SubmitTransactionHelper.ViewUpdate>(error) }
        testObserver.assertUpdates(updates)

        /*
         * Test submit transaction failure
         */
        BDDMockito.given(
            relayRepositoryMock.submit(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                BDDMockito.anyBoolean(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Completable.complete())
        submitEvents.onNext(Unit)
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.TransactionSubmitted(true)) }
        testObserver.assertUpdates(updates)

        /*
         * Test confirmation push message
         */
        BDDMockito.given(
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
        BDDMockito.given(
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
        BDDMockito.given(
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
        BDDMockito.given(
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
        BDDMockito.given(
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
    fun observeAssetTransfer() {
        testFlow(
            TransactionData.AssetTransfer(TEST_ETHER_TOKEN, TEST_ETH_AMOUNT, TEST_ADDRESS),
            { result ->
                result is DataResult &&
                        (result.data as? SubmitTransactionHelper.ViewUpdate.TransactionInfo)?.viewHolder is AssetTransferViewHolder
            }
        )
    }

    @Test
    fun observeGenericTransaction() {
        testFlow(
            TransactionData.Generic(TEST_ETHER_TOKEN, TEST_ETH_AMOUNT, null),
            { result ->
                result is DataResult &&
                        (result.data as? SubmitTransactionHelper.ViewUpdate.TransactionInfo)?.viewHolder is GenericTransactionViewHolder
            }
        )
    }

    @Test
    fun singleOwner() {
        val signatureSubject = PublishSubject.create<Map<Solidity.Address, Signature>>()
        BDDMockito.given(signatureStore.flatMapInfo(MockUtils.any(), MockUtils.any())).willReturn(signatureSubject)
        val pushMessageSubject = PublishSubject.create<PushServiceRepository.TransactionResponse>()
        BDDMockito.given(signaturePushRepository.observe(BDDMockito.anyString())).willReturn(pushMessageSubject)

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
        BDDMockito.given(relayRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(info))

        submitTransactionHelper.observe(events, TransactionData.AssetTransfer(TEST_ETHER_TOKEN, TEST_ETH_AMOUNT, TEST_ADDRESS))
            .subscribe(testObserver)

        val updates = mutableListOf<((Result<SubmitTransactionHelper.ViewUpdate>) -> Boolean)>({ result ->
            result is DataResult &&
                    (result.data as? SubmitTransactionHelper.ViewUpdate.TransactionInfo)?.viewHolder is AssetTransferViewHolder
        })
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.Estimate(Wei(BigInteger.valueOf(32010)), Wei.ether("23"))) }
        testObserver.assertUpdates(updates)
    }

    private fun loadExecutionInfo(transaction: SafeTransaction) =
        relayRepositoryMock.loadExecuteInformation(TEST_SAFE, transaction)

    private fun TestObserver<Result<SubmitTransactionHelper.ViewUpdate>>.assertUpdates(updates: List<((Result<SubmitTransactionHelper.ViewUpdate>) -> Boolean)>): TestObserver<Result<SubmitTransactionHelper.ViewUpdate>> {
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
        private const val TEST_TRANSACTION_HASH = "SomeHash"
        private val TEST_TRANSACTION =
            SafeTransaction(Transaction(Solidity.Address(BigInteger.ZERO), nonce = BigInteger.TEN), TransactionExecutionRepository.Operation.CALL)
        private val TEST_SIGNERS = listOf(BigInteger.valueOf(7), BigInteger.valueOf(13)).map { Solidity.Address(it) }
        private val TEST_OWNERS = TEST_SIGNERS + Solidity.Address(BigInteger.valueOf(5))
        private val TEST_SIGNATURE = Signature(BigInteger.TEN, BigInteger.TEN, 27)
    }
}
