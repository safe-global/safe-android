package pm.gnosis.heimdall.ui.transactions.view.helpers

import com.gojuno.koptional.None
import com.gojuno.koptional.toOptional
import io.reactivex.Completable
import io.reactivex.Observable
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
import pm.gnosis.heimdall.ERC20Contract
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.data.repositories.models.SemVer
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.helpers.SignatureStore
import pm.gnosis.heimdall.ui.transactions.view.TransactionInfoViewHolder
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestObservableFactory
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
    private lateinit var signaturePushRepository: PushServiceRepository

    @Mock
    private lateinit var safeRepository: GnosisSafeRepository

    @Mock
    private lateinit var tokenRepository: TokenRepository

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
        addressHelper = AddressHelper(addressBookRepository)
        submitTransactionHelper =
            DefaultSubmitTransactionHelper(
                relayRepositoryMock, safeRepository, signaturePushRepository, signatureStore,
                tokenRepository, transactionViewHolderBuilder
            )
    }

    @Test
    fun testFlow() {
        val error = TimeoutException()
        val estimationSingleFactory = TestSingleFactory<TransactionExecutionRepository.ExecuteInformation>()
        given(tokenRepository.loadToken(MockUtils.any())).willReturn(Single.just(ERC20Token.ETHER_TOKEN))
        given(relayRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(estimationSingleFactory.get())
        val signatureSubject = PublishSubject.create<Map<Solidity.Address, Signature>>()
        given(signatureStore.flatMapInfo(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(signatureSubject)
        val signatureSingleFactory = TestSingleFactory<Map<Solidity.Address, Signature>>()
        given(signatureStore.load()).willReturn(signatureSingleFactory.get())
        val signaturePublisher = PublishSubject.create<Map<Solidity.Address, Signature>>()
        given(signatureStore.observe()).willReturn(signaturePublisher)
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

        // Estimate, balance too low (Ether transfer)
        val infoTooLowBalanceEth = TransactionExecutionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH,
            TEST_TRANSACTION.copy(wrapped = TEST_TRANSACTION.wrapped.copy(value = Wei.ether("23"))),
            TEST_OWNERS[2], TEST_OWNERS.size - 1, TEST_OWNERS, TEST_VERSION,
            TEST_ETHER_TOKEN, BigInteger.ONE, BigInteger.TEN, BigInteger.ZERO, BigInteger.ZERO,
            Wei.ether("23").value
        )
        given(relayRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(infoTooLowBalanceEth))
        given(transactionViewHolder.loadAssetChange())
            .willReturn(Single.just(ERC20TokenWithBalance(ERC20Token.ETHER_TOKEN, Wei.ether("23").value).toOptional()))
        retryEvents.onNext(Unit)

        then(tokenRepository).should().loadToken(ERC20Token.ETHER_TOKEN.address)
        then(transactionViewHolder).should().loadAssetChange()

        updates += {
            it == DataResult(
                SubmitTransactionHelper.ViewUpdate.Estimate(
                    ERC20TokenWithBalance(ERC20Token.ETHER_TOKEN, BigInteger.valueOf(-10)), BigInteger.TEN, null, false
                )
            )
        }
        testObserver.assertUpdates(updates)

        // Estimate, balance too low (Token transfer)
        val testGasToken = ERC20Token("0x1337".asEthereumAddress()!!, "Gas Token", "GT", 0)
        val infoTooLowBalanceToken = TransactionExecutionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH,
            TEST_TRANSACTION.copy(
                wrapped = TEST_TRANSACTION.wrapped.copy(
                    testGasToken.address,
                    data = ERC20Contract.Transfer.encode(TEST_ADDRESS, Solidity.UInt256(BigInteger.TEN))
                )
            ),
            TEST_OWNERS[2], TEST_OWNERS.size - 1, TEST_OWNERS, TEST_VERSION,
            testGasToken.address, BigInteger.ONE, BigInteger.TEN, BigInteger.ZERO, BigInteger.ZERO,
            BigInteger("19")
        )
        given(relayRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(infoTooLowBalanceToken))
        given(transactionViewHolder.loadAssetChange())
            .willReturn(Single.just(ERC20TokenWithBalance(testGasToken, BigInteger.TEN).toOptional()))
        given(tokenRepository.loadToken(MockUtils.any())).willReturn(Single.just(testGasToken))
        retryEvents.onNext(Unit)

        then(tokenRepository).should().loadToken(testGasToken.address)
        then(transactionViewHolder).should(times(2)).loadAssetChange()

        updates += {
            it == DataResult(
                SubmitTransactionHelper.ViewUpdate.Estimate(
                    ERC20TokenWithBalance(testGasToken, BigInteger.valueOf(-1)), BigInteger.TEN, null, false
                )
            )
        }
        testObserver.assertUpdates(updates)

        // Estimate, asset balance too low (Token transfer)
        val testToken = ERC20Token("0x1337".asEthereumAddress()!!, "Test Token", "TT", 0)
        val assetTooLowBalanceToken = TransactionExecutionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH,
            TEST_TRANSACTION.copy(
                wrapped = TEST_TRANSACTION.wrapped.copy(
                    testToken.address,
                    data = ERC20Contract.Transfer.encode(TEST_ADDRESS, Solidity.UInt256(BigInteger.TEN))
                )
            ),
            TEST_OWNERS[2], TEST_OWNERS.size - 1, TEST_OWNERS, TEST_VERSION,
            TEST_ETHER_TOKEN, BigInteger.ONE, BigInteger.TEN, BigInteger.ZERO, BigInteger.ZERO,
            Wei.ether("23").value
        )
        given(relayRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(assetTooLowBalanceToken))
        given(transactionViewHolder.loadAssetChange())
            .willReturn(Single.just(ERC20TokenWithBalance(testToken, BigInteger.TEN).toOptional()))
        given(tokenRepository.loadToken(MockUtils.any())).willReturn(Single.just(ERC20Token.ETHER_TOKEN))
        given(tokenRepository.loadTokenBalances(MockUtils.any(), MockUtils.any()))
            .willReturn(Observable.just(listOf(testToken to BigInteger.ONE)))
        retryEvents.onNext(Unit)

        then(tokenRepository).should(times(2)).loadToken(TEST_ETHER_TOKEN)
        then(tokenRepository).should().loadTokenBalances(TEST_SAFE, listOf(testToken))
        then(transactionViewHolder).should(times(3)).loadAssetChange()

        updates += {
            it == DataResult(
                SubmitTransactionHelper.ViewUpdate.Estimate(
                    ERC20TokenWithBalance(ERC20Token.ETHER_TOKEN, Wei.ether("23").value - BigInteger.TEN), BigInteger.TEN,
                    ERC20TokenWithBalance(testToken, BigInteger.valueOf(-9)), false
                )
            )
        }
        testObserver.assertUpdates(updates)

        // Estimate, enough funds
        val info = TransactionExecutionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH,
            TEST_TRANSACTION.copy(
                wrapped = TEST_TRANSACTION.wrapped.copy(
                    testGasToken.address,
                    value = Wei.ether("23"),
                    data = ERC20Contract.Transfer.encode(TEST_ADDRESS, Solidity.UInt256(BigInteger.TEN))
                )
            ),
            TEST_OWNERS[2], TEST_OWNERS.size - 1, TEST_OWNERS, TEST_VERSION,
            testGasToken.address, BigInteger.ONE, BigInteger.TEN, BigInteger.ZERO, BigInteger.ZERO,
            BigInteger("21")
        )
        given(relayRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(
            Single.just(
                info
            )
        )
        given(transactionViewHolder.loadAssetChange()).willReturn(Single.just(None))
        retryEvents.onNext(Unit)

        then(tokenRepository).should(times(2)).loadToken(testGasToken.address)
        then(transactionViewHolder).should(times(4)).loadAssetChange()

        updates += {
            it == DataResult(
                SubmitTransactionHelper.ViewUpdate.Estimate(
                    ERC20TokenWithBalance(ERC20Token.ETHER_TOKEN, BigInteger("11")), BigInteger.valueOf(10), null, true
                )
            )
        }
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
                MockUtils.any(),
                MockUtils.any(),
                anySet()
            )
        )
            .willReturn(Completable.error(error))
        signaturePublisher.onNext(emptyMap())
        updates += {
            it == DataResult(
                SubmitTransactionHelper.ViewUpdate.RequireConfirmations(
                    AuthenticatorInfo(AuthenticatorInfo.Type.EXTENSION, TEST_OWNERS[0]),
                    TEST_TRANSACTION_HASH
                )
            )
        }
        testObserver.assertUpdates(updates)
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
                info.operationalGas,
                info.gasPrice,
                info.gasToken,
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
                info.operationalGas,
                info.gasPrice,
                info.gasToken,
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
                MockUtils.any(),
                MockUtils.any(),
                anyBoolean(),
                MockUtils.any()
            )
        ).willReturn(Single.error(error))
        submitEvents.onNext(Unit)
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.TransactionSubmitted(null)) }
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
                MockUtils.any(),
                MockUtils.any(),
                anyBoolean(),
                MockUtils.any()
            )
        ).willReturn(Single.just(TEST_CHAIN_HASH))
        given(
            signaturePushRepository.propagateSubmittedTransaction(anyString(), anyString(), MockUtils.any(), anySet())
        ).willReturn(Completable.error(TimeoutException()))
        submitEvents.onNext(Unit)
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.TransactionSubmitted(TEST_CHAIN_HASH)) }
        testObserver.assertUpdates(updates)
        then(signaturePushRepository).should()
            .propagateSubmittedTransaction(TEST_TRANSACTION_HASH, TEST_CHAIN_HASH, TEST_SAFE, setOf(TEST_OWNERS[0], TEST_OWNERS[1]))

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
                MockUtils.any(),
                MockUtils.any(),
                anyBoolean(),
                MockUtils.any()
            )
        ).willReturn(Single.just(TEST_CHAIN_HASH))
        given(
            signaturePushRepository.propagateSubmittedTransaction(anyString(), anyString(), MockUtils.any(), anySet())
        ).willReturn(Completable.complete())
        submitEvents.onNext(Unit)
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.TransactionSubmitted(TEST_CHAIN_HASH)) }
        testObserver.assertUpdates(updates)
        then(signaturePushRepository).should(times(2))
            .propagateSubmittedTransaction(TEST_TRANSACTION_HASH, TEST_CHAIN_HASH, TEST_SAFE, setOf(TEST_OWNERS[0], TEST_OWNERS[1]))

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
        given(tokenRepository.loadToken(MockUtils.any())).willReturn(Single.just(ERC20Token.ETHER_TOKEN))
        given(transactionViewHolder.loadAssetChange()).willReturn(Single.just(None))
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
            TEST_TRANSACTION, TEST_OWNERS[0], 1, TEST_OWNERS.subList(0, 1), TEST_VERSION,
            TEST_ETHER_TOKEN, BigInteger.ONE, BigInteger.TEN, BigInteger.ZERO, BigInteger.ZERO,
            Wei.ether("23").value
        )
        given(relayRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(info))

        submitTransactionHelper.observe(events, TransactionData.AssetTransfer(TEST_ETHER_TOKEN, TEST_ETH_AMOUNT, TEST_ADDRESS))
            .subscribe(testObserver)

        val updates = mutableListOf<((Result<SubmitTransactionHelper.ViewUpdate>) -> Boolean)>({
            ((it as? DataResult)?.data as? SubmitTransactionHelper.ViewUpdate.TransactionInfo)?.viewHolder == transactionViewHolder
        })
        updates += {
            it == DataResult(
                SubmitTransactionHelper.ViewUpdate.Estimate(
                    ERC20TokenWithBalance(ERC20Token.ETHER_TOKEN, Wei.ether("23").value - BigInteger.TEN), BigInteger.TEN, null, true
                )
            )
        }
        testObserver.assertUpdates(updates)

        signatureSubject.onNext(emptyMap())
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.Confirmations(true)) }
        testObserver.assertUpdates(updates)
    }

    @Test
    fun initialSignatures() {
        given(transactionViewHolder.loadAssetChange()).willReturn(Single.just(None))
        given(tokenRepository.loadToken(MockUtils.any())).willReturn(Single.just(ERC20Token.ETHER_TOKEN))
        val signatureSubject = PublishSubject.create<Map<Solidity.Address, Signature>>()
        given(signatureStore.flatMapInfo(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(signatureSubject)
        val pushMessageSubject = PublishSubject.create<PushServiceRepository.TransactionResponse>()
        given(signaturePushRepository.observe(anyString())).willReturn(pushMessageSubject)
        given(
            relayRepositoryMock.checkConfirmation(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.just(TEST_OWNERS[0] to TEST_SIGNATURE))

        submitTransactionHelper.setup(TEST_SAFE, ::loadExecutionInfo)
        val retryEvents = PublishSubject.create<Unit>()
        val requestEvents = PublishSubject.create<Unit>()
        val submitEvents = PublishSubject.create<Unit>()
        val events = SubmitTransactionHelper.Events(
            retryEvents, requestEvents, submitEvents
        )
        val testObserver = TestObserver<Result<SubmitTransactionHelper.ViewUpdate>>()

        val info = TransactionExecutionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH, TEST_TRANSACTION,
            TEST_OWNERS[2], TEST_OWNERS.size - 1, TEST_OWNERS, TEST_VERSION,
            TEST_ETHER_TOKEN, BigInteger.ONE, BigInteger.TEN, BigInteger.ZERO, BigInteger.ZERO,
            Wei.ether("23").value
        )
        given(relayRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(info))

        val initialSignatures = setOf(TEST_SIGNATURE)

        submitTransactionHelper.observe(events, TransactionData.AssetTransfer(TEST_ETHER_TOKEN, TEST_ETH_AMOUNT, TEST_ADDRESS), initialSignatures)
            .subscribe(testObserver)

        val updates = mutableListOf<((Result<SubmitTransactionHelper.ViewUpdate>) -> Boolean)>({
            ((it as? DataResult)?.data as? SubmitTransactionHelper.ViewUpdate.TransactionInfo)?.viewHolder == transactionViewHolder
        })
        updates += {
            it == DataResult(
                SubmitTransactionHelper.ViewUpdate.Estimate(
                    ERC20TokenWithBalance(ERC20Token.ETHER_TOKEN, Wei.ether("23").value - BigInteger.TEN), BigInteger.TEN, null, true
                )
            )
        }
        testObserver.assertUpdates(updates)

        signatureSubject.onNext(mapOf(TEST_OWNERS[0] to TEST_SIGNATURE))
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.Confirmations(true)) }
        testObserver.assertUpdates(updates)

        then(signatureStore).should().flatMapInfo(MockUtils.eq(TEST_SAFE), MockUtils.eq(info), MockUtils.eq(mapOf(TEST_OWNERS[0] to TEST_SIGNATURE)))
    }

    @Test
    fun initialSignaturesCheckError() {
        given(transactionViewHolder.loadAssetChange()).willReturn(Single.just(None))
        given(tokenRepository.loadToken(MockUtils.any())).willReturn(Single.just(ERC20Token.ETHER_TOKEN))
        val signatureSubject = PublishSubject.create<Map<Solidity.Address, Signature>>()
        given(signatureStore.flatMapInfo(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(signatureSubject)
        // Return a observable that will not emit an value
        val testObservableFactory = TestObservableFactory<Map<Solidity.Address, Signature>>()
        given(signatureStore.observe()).willReturn(testObservableFactory.get())
        val pushMessageSubject = PublishSubject.create<PushServiceRepository.TransactionResponse>()
        given(signaturePushRepository.observe(anyString())).willReturn(pushMessageSubject)
        given(
            relayRepositoryMock.checkConfirmation(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.error(IllegalStateException()))

        submitTransactionHelper.setup(TEST_SAFE, ::loadExecutionInfo)
        val retryEvents = PublishSubject.create<Unit>()
        val requestEvents = PublishSubject.create<Unit>()
        val submitEvents = PublishSubject.create<Unit>()
        val events = SubmitTransactionHelper.Events(
            retryEvents, requestEvents, submitEvents
        )
        val testObserver = TestObserver<Result<SubmitTransactionHelper.ViewUpdate>>()

        val info = TransactionExecutionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH, TEST_TRANSACTION,
            TEST_OWNERS[2], TEST_OWNERS.size - 1, TEST_OWNERS, TEST_VERSION,
            TEST_ETHER_TOKEN, BigInteger.ONE, BigInteger.TEN, BigInteger.ZERO, BigInteger.ZERO,
            Wei.ether("23").value
        )
        given(relayRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(info))

        val initialSignatures = setOf(TEST_SIGNATURE)

        submitTransactionHelper.observe(events, TransactionData.AssetTransfer(TEST_ETHER_TOKEN, TEST_ETH_AMOUNT, TEST_ADDRESS), initialSignatures)
            .subscribe(testObserver)

        val updates = mutableListOf<((Result<SubmitTransactionHelper.ViewUpdate>) -> Boolean)>({
            ((it as? DataResult)?.data as? SubmitTransactionHelper.ViewUpdate.TransactionInfo)?.viewHolder == transactionViewHolder
        })
        updates += {
            it == DataResult(
                SubmitTransactionHelper.ViewUpdate.Estimate(
                    ERC20TokenWithBalance(ERC20Token.ETHER_TOKEN, Wei.ether("23").value - BigInteger.TEN), BigInteger.TEN, null, true
                )
            )
        }
        testObserver.assertUpdates(updates)

        signatureSubject.onNext(emptyMap())
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.Confirmations(false)) }
        testObserver.assertUpdates(updates)
        testObservableFactory.assertCount(1)

        then(signatureStore).should().flatMapInfo(MockUtils.eq(TEST_SAFE), MockUtils.eq(info), MockUtils.eq(emptyMap()))
    }

    @Test
    fun initialSignaturesUnknownOwner() {
        given(transactionViewHolder.loadAssetChange()).willReturn(Single.just(None))
        given(tokenRepository.loadToken(MockUtils.any())).willReturn(Single.just(ERC20Token.ETHER_TOKEN))
        val signatureSubject = PublishSubject.create<Map<Solidity.Address, Signature>>()
        given(signatureStore.flatMapInfo(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(signatureSubject)
        // Return a single that will not emit an value
        val testObservableFactory = TestObservableFactory<Map<Solidity.Address, Signature>>()
        given(signatureStore.observe()).willReturn(testObservableFactory.get())
        val pushMessageSubject = PublishSubject.create<PushServiceRepository.TransactionResponse>()
        given(signaturePushRepository.observe(anyString())).willReturn(pushMessageSubject)
        given(
            relayRepositoryMock.checkConfirmation(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.just(Solidity.Address(BigInteger.ONE) to TEST_SIGNATURE))

        submitTransactionHelper.setup(TEST_SAFE, ::loadExecutionInfo)
        val retryEvents = PublishSubject.create<Unit>()
        val requestEvents = PublishSubject.create<Unit>()
        val submitEvents = PublishSubject.create<Unit>()
        val events = SubmitTransactionHelper.Events(
            retryEvents, requestEvents, submitEvents
        )
        val testObserver = TestObserver<Result<SubmitTransactionHelper.ViewUpdate>>()

        val info = TransactionExecutionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH, TEST_TRANSACTION,
            TEST_OWNERS[2], TEST_OWNERS.size - 1, TEST_OWNERS, TEST_VERSION,
            TEST_ETHER_TOKEN, BigInteger.ONE, BigInteger.TEN, BigInteger.ZERO, BigInteger.ZERO,
            Wei.ether("23").value
        )
        given(relayRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(info))

        val initialSignatures = setOf(TEST_SIGNATURE)

        submitTransactionHelper.observe(events, TransactionData.AssetTransfer(TEST_ETHER_TOKEN, TEST_ETH_AMOUNT, TEST_ADDRESS), initialSignatures)
            .subscribe(testObserver)

        val updates = mutableListOf<((Result<SubmitTransactionHelper.ViewUpdate>) -> Boolean)>({
            ((it as? DataResult)?.data as? SubmitTransactionHelper.ViewUpdate.TransactionInfo)?.viewHolder == transactionViewHolder
        })
        updates += {
            it == DataResult(
                SubmitTransactionHelper.ViewUpdate.Estimate(
                    ERC20TokenWithBalance(ERC20Token.ETHER_TOKEN, Wei.ether("23").value - BigInteger.TEN), BigInteger.TEN, null, true
                )
            )
        }
        testObserver.assertUpdates(updates)

        signatureSubject.onNext(emptyMap())
        updates += { it == DataResult(SubmitTransactionHelper.ViewUpdate.Confirmations(false)) }
        testObserver.assertUpdates(updates)
        testObservableFactory.assertCount(1)

        then(signatureStore).should().flatMapInfo(
            MockUtils.eq(TEST_SAFE), MockUtils.eq(info), MockUtils.eq(mapOf(Solidity.Address(BigInteger.ONE) to TEST_SIGNATURE))
        )
    }

    private fun loadExecutionInfo(transaction: SafeTransaction) =
        relayRepositoryMock.loadExecuteInformation(TEST_SAFE, TEST_ETHER_TOKEN, transaction)

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
        private const val TEST_CHAIN_HASH = "SomeChainHash"
        private const val TEST_TRANSACTION_HASH = "SomeHash"
        private val TEST_TRANSACTION =
            SafeTransaction(Transaction(Solidity.Address(BigInteger.ZERO), nonce = BigInteger.TEN), TransactionExecutionRepository.Operation.CALL)
        private val TEST_SIGNERS = listOf(BigInteger.valueOf(7), BigInteger.valueOf(13)).map { Solidity.Address(it) }
        private val TEST_OWNERS = TEST_SIGNERS + Solidity.Address(BigInteger.valueOf(5))
        private val TEST_SIGNATURE = Signature(BigInteger.TEN, BigInteger.TEN, 27)
        private val TEST_VERSION = SemVer(1, 0, 0)
    }
}
