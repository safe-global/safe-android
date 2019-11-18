package pm.gnosis.heimdall.ui.safe.recover.authenticator

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Predicate
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.*
import pm.gnosis.heimdall.helpers.CryptoHelper
import pm.gnosis.heimdall.ui.safe.pairing.PairingSubmitContract
import pm.gnosis.heimdall.ui.safe.pairing.PairingSubmitViewModel
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.asOwner
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
class PairingSubmitViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var cryptoHelper: CryptoHelper

    @Mock
    private lateinit var gnosisSafeRepositoryMock: GnosisSafeRepository

    @Mock
    private lateinit var pushServiceRepositoryMock: PushServiceRepository

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    @Mock
    private lateinit var transactionExecutionRepositoryMock: TransactionExecutionRepository

    private lateinit var viewModel: PairingSubmitViewModel

    @Before
    fun setUp() {
        viewModel = PairingSubmitViewModel(
            cryptoHelper,
            gnosisSafeRepositoryMock,
            pushServiceRepositoryMock,
            tokenRepositoryMock,
            transactionExecutionRepositoryMock
        )
    }

    @Test
    fun getMaxTransactionFee() {
        given(tokenRepositoryMock.loadToken(MockUtils.any())).willReturn(Single.just(GAS_TOKEN))
        viewModel.setup(
            SAFE_TRANSACTION,
            SIGNATURE_1,
            SIGNATURE_2,
            TX_GAS,
            DATA_GAS,
            OPERATIONAL_GAS,
            GAS_PRICE,
            GAS_TOKEN.address,
            NEW_AUTHENTICATOR_INFO,
            TX_HASH
        )
        val testObserver = TestObserver<ERC20TokenWithBalance>()
        viewModel.loadFeeInfo().subscribe(testObserver)
        testObserver.assertValue { it.balance == 18000000.toBigInteger() && it.token == GAS_TOKEN }
        then(tokenRepositoryMock).should().loadToken(GAS_TOKEN.address)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun getMaxTransactionFeeError() {
        val error = IllegalStateException()
        given(tokenRepositoryMock.loadToken(MockUtils.any())).willReturn(Single.error(error))
        viewModel.setup(
            SAFE_TRANSACTION,
            SIGNATURE_1,
            SIGNATURE_2,
            TX_GAS,
            DATA_GAS,
            OPERATIONAL_GAS,
            GAS_PRICE,
            GAS_TOKEN.address,
            NEW_AUTHENTICATOR_INFO,
            TX_HASH
        )
        val testObserver = TestObserver<ERC20TokenWithBalance>()
        viewModel.loadFeeInfo().subscribe(testObserver)
        testObserver.assertFailure(Predicate { it == error })
        then(tokenRepositoryMock).should().loadToken(GAS_TOKEN.address)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeSafeBalance() {
        val testScheduler = TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { _ -> testScheduler }

        val testObserver = TestObserver.create<Result<PairingSubmitContract.SubmitStatus>>()
        val safeAddress = SAFE_TRANSACTION.wrapped.address
        val token = listOf(GAS_TOKEN)
        var balanceToReturn: BigInteger? = BigInteger.ZERO

        given(tokenRepositoryMock.loadToken(MockUtils.any())).willReturn(Single.just(GAS_TOKEN))
        given(
            tokenRepositoryMock.loadTokenBalances(
                safeAddress,
                token
            )
        ).willAnswer {
            Observable.fromCallable {
                if (balanceToReturn == null) emptyList()
                else listOf(GAS_TOKEN to balanceToReturn)
            }
        }

        viewModel.setup(
            SAFE_TRANSACTION,
            SIGNATURE_1,
            SIGNATURE_2,
            TX_GAS,
            DATA_GAS,
            OPERATIONAL_GAS,
            GAS_PRICE,
            GAS_TOKEN.address,
            NEW_AUTHENTICATOR_INFO,
            TX_HASH
        )
        viewModel.observeSubmitStatus().subscribe(testObserver)
        then(tokenRepositoryMock).should().loadToken(GAS_TOKEN.address)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()

        // First successful emission
        testScheduler.advanceTimeBy(0, TimeUnit.SECONDS)
        then(tokenRepositoryMock).should().loadTokenBalances(safeAddress, token)
        testObserver.assertValueAt(
            0, DataResult(PairingSubmitContract.SubmitStatus(ERC20TokenWithBalance(GAS_TOKEN, (-18000000).toBigInteger()), ERC20TokenWithBalance(GAS_TOKEN, (-36000000).toBigInteger()), false))
        )

        // Second emission with error
        balanceToReturn = null
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        then(tokenRepositoryMock).should(times(2)).loadTokenBalances(safeAddress, token)
        testObserver.assertValueAt(1) { it is ErrorResult && it.error is PairingSubmitContract.NoTokenBalanceException }

        // Third successful emission
        balanceToReturn = 18000000.toBigInteger()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        then(tokenRepositoryMock).should(times(3)).loadTokenBalances(safeAddress, token)
        testObserver.assertValueAt(
            2, DataResult(PairingSubmitContract.SubmitStatus(ERC20TokenWithBalance(GAS_TOKEN, BigInteger.ZERO), ERC20TokenWithBalance(GAS_TOKEN, (-18000000).toBigInteger()), true))
        )
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()

        testObserver.assertValueCount(3)
    }

    @Test
    fun observeSafeBalancePaymentTokenError() {
        val testScheduler = TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { _ -> testScheduler }

        val testObserver = TestObserver.create<Result<PairingSubmitContract.SubmitStatus>>()
        val exception = Exception()

        given(tokenRepositoryMock.loadToken(MockUtils.any())).willReturn(Single.error(exception))

        viewModel.setup(
            SAFE_TRANSACTION,
            SIGNATURE_1,
            SIGNATURE_2,
            TX_GAS,
            DATA_GAS,
            OPERATIONAL_GAS,
            GAS_PRICE,
            GAS_TOKEN.address,
            NEW_AUTHENTICATOR_INFO,
            TX_HASH
        )
        viewModel.observeSubmitStatus().subscribe(testObserver)
        testObserver.assertFailure(Predicate { it == exception })
        then(tokenRepositoryMock).should().loadToken(GAS_TOKEN.address)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeSafeBalanceError() {
        val testScheduler = TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { _ -> testScheduler }

        val testObserver = TestObserver.create<Result<PairingSubmitContract.SubmitStatus>>()
        val safeAddress = SAFE_TRANSACTION.wrapped.address
        val token = listOf(GAS_TOKEN)
        val exception = Exception()

        given(tokenRepositoryMock.loadToken(MockUtils.any())).willReturn(Single.just(GAS_TOKEN))
        given(
            tokenRepositoryMock.loadTokenBalances(
                safeAddress,
                token
            )
        ).willReturn(Observable.error(exception))

        viewModel.setup(
            SAFE_TRANSACTION,
            SIGNATURE_1,
            SIGNATURE_2,
            TX_GAS,
            DATA_GAS,
            OPERATIONAL_GAS,
            GAS_PRICE,
            GAS_TOKEN.address,
            NEW_AUTHENTICATOR_INFO,
            TX_HASH
        )
        viewModel.observeSubmitStatus().subscribe(testObserver)

        testScheduler.advanceTimeBy(0, TimeUnit.SECONDS)
        testObserver.assertValue(ErrorResult(exception))
            .assertNoErrors()
            .assertNotTerminated()
        then(tokenRepositoryMock).should().loadToken(GAS_TOKEN.address)
        then(tokenRepositoryMock).should().loadTokenBalances(safeAddress, token)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun getSafeTransaction() {
        viewModel.setup(
            SAFE_TRANSACTION,
            SIGNATURE_1,
            SIGNATURE_2,
            TX_GAS,
            DATA_GAS,
            OPERATIONAL_GAS,
            GAS_PRICE,
            GAS_TOKEN.address,
            NEW_AUTHENTICATOR_INFO,
            TX_HASH
        )
        assertEquals(SAFE_TRANSACTION, viewModel.getSafeTransaction())
    }

    @Test
    fun loadInfo() {
        val testObserver = TestObserver<Safe>()
        val safe = Safe(SAFE_TRANSACTION.wrapped.address)
        given(gnosisSafeRepositoryMock.loadSafe(MockUtils.any())).willReturn(Single.just(safe))

        viewModel.setup(
            SAFE_TRANSACTION,
            SIGNATURE_1,
            SIGNATURE_2,
            TX_GAS,
            DATA_GAS,
            OPERATIONAL_GAS,
            GAS_PRICE,
            GAS_TOKEN.address,
            NEW_AUTHENTICATOR_INFO,
            TX_HASH
        )
        viewModel.loadSafe().subscribe(testObserver)

        then(gnosisSafeRepositoryMock).should().loadSafe(SAFE_TRANSACTION.wrapped.address)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(safe)
    }

    @Test
    fun loadSafeError() {
        val testObserver = TestObserver<Safe>()
        val exception = Exception()
        given(gnosisSafeRepositoryMock.loadSafe(MockUtils.any())).willReturn(Single.error(exception))

        viewModel.setup(
            SAFE_TRANSACTION,
            SIGNATURE_1,
            SIGNATURE_2,
            TX_GAS,
            DATA_GAS,
            OPERATIONAL_GAS,
            GAS_PRICE,
            GAS_TOKEN.address,
            NEW_AUTHENTICATOR_INFO,
            TX_HASH
        )
        viewModel.loadSafe().subscribe(testObserver)

        then(gnosisSafeRepositoryMock).should().loadSafe(SAFE_TRANSACTION.wrapped.address)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertFailure(Exception::class.java)
    }

    @Test
    fun submitTransaction() {
        val testObserver = TestObserver.create<Result<Unit>>()
        given(
            transactionExecutionRepositoryMock.calculateHash(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.just(TX_HASH))


        given(
            cryptoHelper.recover(
                TX_HASH,
                SIGNATURE_1
            )
        ).willReturn("0x0a".asEthereumAddress()!!)
        given(
            cryptoHelper.recover(
                TX_HASH,
                SIGNATURE_2
            )
        ).willReturn("0x14".asEthereumAddress()!!)

        given(
            transactionExecutionRepositoryMock.submit(
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
        ).willReturn(Single.just("RANDOM_TX_HASH"))

        given(gnosisSafeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.just(
            SafeInfo(SAFE_TRANSACTION.wrapped.address, Wei.ZERO, 1, emptyList(), false, emptyList(), VERSION)
        ))

        given(pushServiceRepositoryMock.propagateSafeCreation(MockUtils.any(), MockUtils.any())).willReturn(Completable.complete())

        viewModel.setup(
            SAFE_TRANSACTION,
            SIGNATURE_1,
            SIGNATURE_2,
            TX_GAS,
            DATA_GAS,
            OPERATIONAL_GAS,
            GAS_PRICE,
            GAS_TOKEN.address,
            NEW_AUTHENTICATOR_INFO,
            TX_HASH
        )
        viewModel.submitTransaction().subscribe(testObserver)

        then(transactionExecutionRepositoryMock).should()
            .calculateHash(
                SAFE_TRANSACTION.wrapped.address,
                SAFE_TRANSACTION,
                TX_GAS,
                DATA_GAS,
                GAS_PRICE,
                GAS_TOKEN.address,
                VERSION
            )
        then(cryptoHelper).should().recover(
            TX_HASH,
            SIGNATURE_1
        )
        then(cryptoHelper).should().recover(
            TX_HASH,
            SIGNATURE_2
        )
        then(transactionExecutionRepositoryMock).should().submit(
            SAFE_TRANSACTION.wrapped.address,
            SAFE_TRANSACTION,
            mapOf(Solidity.Address(10.toBigInteger()) to SIGNATURE_1, Solidity.Address(20.toBigInteger()) to SIGNATURE_2),
            false,
            TX_GAS,
            DATA_GAS,
            GAS_PRICE,
            GAS_TOKEN.address,
            VERSION,
            true
        )
        then(pushServiceRepositoryMock).should().propagateSafeCreation(
            SAFE_TRANSACTION.wrapped.address, setOf(
                NEW_AUTHENTICATOR_INFO.authenticator.address
            )
        )
        then(gnosisSafeRepositoryMock).should().loadInfo(SAFE_TRANSACTION.wrapped.address)
        then(gnosisSafeRepositoryMock).should().saveAuthenticatorInfo(NEW_AUTHENTICATOR_INFO.authenticator)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(transactionExecutionRepositoryMock).shouldHaveNoMoreInteractions()
        then(cryptoHelper).shouldHaveNoMoreInteractions()
        then(pushServiceRepositoryMock).shouldHaveNoMoreInteractions()

        testObserver.assertResult(DataResult(Unit))
    }

    @Test
    fun submitTransactionKeycard() {
        val testObserver = TestObserver.create<Result<Unit>>()
        given(
            transactionExecutionRepositoryMock.calculateHash(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.just(TX_HASH))

        given(
            cryptoHelper.recover(
                TX_HASH,
                SIGNATURE_1
            )
        ).willReturn("0x0a".asEthereumAddress()!!)
        given(
            cryptoHelper.recover(
                TX_HASH,
                SIGNATURE_2
            )
        ).willReturn("0x14".asEthereumAddress()!!)

        given(
            transactionExecutionRepositoryMock.submit(
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
        ).willReturn(Single.just("RANDOM_TX_HASH"))

        given(gnosisSafeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.just(
            SafeInfo(SAFE_TRANSACTION.wrapped.address, Wei.ZERO, 1, emptyList(), false, emptyList(), VERSION)
        ))

        val authenticator = NEW_AUTHENTICATOR_INFO.authenticator.copy(type = AuthenticatorInfo.Type.KEYCARD, keyIndex = 1105)
        viewModel.setup(
            SAFE_TRANSACTION,
            SIGNATURE_1,
            SIGNATURE_2,
            TX_GAS,
            DATA_GAS,
            OPERATIONAL_GAS,
            GAS_PRICE,
            GAS_TOKEN.address,
            NEW_AUTHENTICATOR_INFO.copy(authenticator = authenticator),
            TX_HASH
        )
        viewModel.submitTransaction().subscribe(testObserver)

        then(transactionExecutionRepositoryMock).should()
            .calculateHash(
                SAFE_TRANSACTION.wrapped.address,
                SAFE_TRANSACTION,
                TX_GAS,
                DATA_GAS,
                GAS_PRICE,
                GAS_TOKEN.address,
                VERSION
            )
        then(cryptoHelper).should().recover(
            TX_HASH,
            SIGNATURE_1
        )
        then(cryptoHelper).should().recover(
            TX_HASH,
            SIGNATURE_2
        )
        then(transactionExecutionRepositoryMock).should().submit(
            SAFE_TRANSACTION.wrapped.address,
            SAFE_TRANSACTION,
            mapOf(Solidity.Address(10.toBigInteger()) to SIGNATURE_1, Solidity.Address(20.toBigInteger()) to SIGNATURE_2),
            false,
            TX_GAS,
            DATA_GAS,
            GAS_PRICE,
            GAS_TOKEN.address,
            VERSION,
            true
        )
        then(gnosisSafeRepositoryMock).should().loadInfo(SAFE_TRANSACTION.wrapped.address)
        then(gnosisSafeRepositoryMock).should().saveAuthenticatorInfo(authenticator)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(transactionExecutionRepositoryMock).shouldHaveNoMoreInteractions()
        then(cryptoHelper).shouldHaveNoMoreInteractions()
        then(pushServiceRepositoryMock).shouldHaveZeroInteractions()

        testObserver.assertResult(DataResult(Unit))
    }

    @Test
    fun submitTransactionErrorSendingPush() {
        val testObserver = TestObserver.create<Result<Unit>>()
        val exception = Exception()
        given(
            transactionExecutionRepositoryMock.calculateHash(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.just(TX_HASH))

        given(
            cryptoHelper.recover(
                TX_HASH,
                SIGNATURE_1
            )
        ).willReturn("0x0a".asEthereumAddress()!!)
        given(
            cryptoHelper.recover(
                TX_HASH,
                SIGNATURE_2
            )
        ).willReturn("0x14".asEthereumAddress()!!)

        given(
            transactionExecutionRepositoryMock.submit(
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
        ).willReturn(Single.just("RANDOM_TX_HASH"))

        given(gnosisSafeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.just(
            SafeInfo(SAFE_TRANSACTION.wrapped.address, Wei.ZERO, 1, emptyList(), false, emptyList(), VERSION)
        ))

        given(pushServiceRepositoryMock.propagateSafeCreation(MockUtils.any(), MockUtils.any())).willReturn(Completable.error(exception))

        viewModel.setup(
            SAFE_TRANSACTION,
            SIGNATURE_1,
            SIGNATURE_2,
            TX_GAS,
            DATA_GAS,
            OPERATIONAL_GAS,
            GAS_PRICE,
            GAS_TOKEN.address,
            NEW_AUTHENTICATOR_INFO,
            TX_HASH
        )
        viewModel.submitTransaction().subscribe(testObserver)

        then(transactionExecutionRepositoryMock).should()
            .calculateHash(
                SAFE_TRANSACTION.wrapped.address,
                SAFE_TRANSACTION,
                TX_GAS,
                DATA_GAS,
                GAS_PRICE,
                GAS_TOKEN.address,
                VERSION
            )
        then(cryptoHelper).should().recover(
            TX_HASH,
            SIGNATURE_1
        )
        then(cryptoHelper).should().recover(
            TX_HASH,
            SIGNATURE_2
        )
        then(transactionExecutionRepositoryMock).should().submit(
            SAFE_TRANSACTION.wrapped.address,
            SAFE_TRANSACTION,
            mapOf(Solidity.Address(10.toBigInteger()) to SIGNATURE_1, Solidity.Address(20.toBigInteger()) to SIGNATURE_2),
            false,
            TX_GAS,
            DATA_GAS,
            GAS_PRICE,
            GAS_TOKEN.address,
            VERSION,
            true
        )
        then(pushServiceRepositoryMock).should().propagateSafeCreation(
            SAFE_TRANSACTION.wrapped.address, setOf(
                NEW_AUTHENTICATOR_INFO.authenticator.address
            )
        )
        then(gnosisSafeRepositoryMock).should().loadInfo(SAFE_TRANSACTION.wrapped.address)
        then(gnosisSafeRepositoryMock).should().saveAuthenticatorInfo(NEW_AUTHENTICATOR_INFO.authenticator)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(transactionExecutionRepositoryMock).shouldHaveNoMoreInteractions()
        then(cryptoHelper).shouldHaveNoMoreInteractions()
        then(pushServiceRepositoryMock).shouldHaveNoMoreInteractions()

        testObserver.assertResult(DataResult(Unit))
    }

    @Test
    fun submitTransactionErrorSubmitting() {
        val testObserver = TestObserver.create<Result<Unit>>()
        val exception = Exception()
        given(
            transactionExecutionRepositoryMock.calculateHash(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.just(TX_HASH))

        given(
            cryptoHelper.recover(
                TX_HASH,
                SIGNATURE_1
            )
        ).willReturn("0x0a".asEthereumAddress()!!)
        given(
            cryptoHelper.recover(
                TX_HASH,
                SIGNATURE_2
            )
        ).willReturn("0x14".asEthereumAddress()!!)

        given(
            transactionExecutionRepositoryMock.submit(
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
        ).willReturn(Single.error(exception))

        given(gnosisSafeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.just(
            SafeInfo(SAFE_TRANSACTION.wrapped.address, Wei.ZERO, 1, emptyList(), false, emptyList(), VERSION)
        ))

        viewModel.setup(
            SAFE_TRANSACTION,
            SIGNATURE_1,
            SIGNATURE_2,
            TX_GAS,
            DATA_GAS,
            OPERATIONAL_GAS,
            GAS_PRICE,
            GAS_TOKEN.address,
            NEW_AUTHENTICATOR_INFO,
            TX_HASH
        )
        viewModel.submitTransaction().subscribe(testObserver)

        then(transactionExecutionRepositoryMock).should()
            .calculateHash(
                SAFE_TRANSACTION.wrapped.address,
                SAFE_TRANSACTION,
                TX_GAS,
                DATA_GAS,
                GAS_PRICE,
                GAS_TOKEN.address,
                VERSION
            )
        then(cryptoHelper).should().recover(
            TX_HASH,
            SIGNATURE_1
        )
        then(cryptoHelper).should().recover(
            TX_HASH,
            SIGNATURE_2
        )
        then(transactionExecutionRepositoryMock).should().submit(
            SAFE_TRANSACTION.wrapped.address,
            SAFE_TRANSACTION,
            mapOf(Solidity.Address(10.toBigInteger()) to SIGNATURE_1, Solidity.Address(20.toBigInteger()) to SIGNATURE_2),
            false,
            TX_GAS,
            DATA_GAS,
            GAS_PRICE,
            GAS_TOKEN.address,
            VERSION,
            true
        )
        then(gnosisSafeRepositoryMock).should().loadInfo(SAFE_TRANSACTION.wrapped.address)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(transactionExecutionRepositoryMock).shouldHaveNoMoreInteractions()
        then(cryptoHelper).shouldHaveNoMoreInteractions()
        then(pushServiceRepositoryMock).shouldHaveZeroInteractions()

        testObserver.assertValue { it is ErrorResult && it.error == exception }
    }

    @Test
    fun submitTransactionErrorRecovering() {
        val testObserver = TestObserver.create<Result<Unit>>()
        val exception = RuntimeException()
        given(
            transactionExecutionRepositoryMock.calculateHash(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.just(TX_HASH))

        given(
            cryptoHelper.recover(
                TX_HASH,
                SIGNATURE_1
            )
        ).willReturn("0x0a".asEthereumAddress())
        given(
            cryptoHelper.recover(
                TX_HASH,
                SIGNATURE_2
            )
        ).willThrow(exception)

        given(gnosisSafeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.just(
            SafeInfo(SAFE_TRANSACTION.wrapped.address, Wei.ZERO, 1, emptyList(), false, emptyList(), VERSION)
        ))

        viewModel.setup(
            SAFE_TRANSACTION,
            SIGNATURE_1,
            SIGNATURE_2,
            TX_GAS,
            DATA_GAS,
            OPERATIONAL_GAS,
            GAS_PRICE,
            GAS_TOKEN.address,
            NEW_AUTHENTICATOR_INFO,
            TX_HASH
        )
        viewModel.submitTransaction().subscribe(testObserver)

        then(transactionExecutionRepositoryMock).should()
            .calculateHash(
                SAFE_TRANSACTION.wrapped.address,
                SAFE_TRANSACTION,
                TX_GAS,
                DATA_GAS,
                GAS_PRICE,
                GAS_TOKEN.address,
                VERSION
            )
        then(cryptoHelper).should().recover(
            TX_HASH,
            SIGNATURE_1
        )
        then(cryptoHelper).should().recover(
            TX_HASH,
            SIGNATURE_2
        )
        then(gnosisSafeRepositoryMock).should().loadInfo(SAFE_TRANSACTION.wrapped.address)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(transactionExecutionRepositoryMock).shouldHaveNoMoreInteractions()
        then(cryptoHelper).shouldHaveNoMoreInteractions()
        then(pushServiceRepositoryMock).shouldHaveZeroInteractions()

        testObserver.assertValue { it is ErrorResult && it.error == exception }
    }

    @Test
    fun submitTransactionWrongHash() {
        val testObserver = TestObserver.create<Result<Unit>>()
        given(
            transactionExecutionRepositoryMock.calculateHash(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.just(byteArrayOf(0, 0, 0)))

        given(gnosisSafeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.just(
            SafeInfo(SAFE_TRANSACTION.wrapped.address, Wei.ZERO, 1, emptyList(), false, emptyList(), VERSION)
        ))

        viewModel.setup(
            SAFE_TRANSACTION,
            SIGNATURE_1,
            SIGNATURE_2,
            TX_GAS,
            DATA_GAS,
            OPERATIONAL_GAS,
            GAS_PRICE,
            GAS_TOKEN.address,
            NEW_AUTHENTICATOR_INFO,
            TX_HASH
        )
        viewModel.submitTransaction().subscribe(testObserver)

        then(transactionExecutionRepositoryMock).should()
            .calculateHash(
                SAFE_TRANSACTION.wrapped.address,
                SAFE_TRANSACTION,
                TX_GAS,
                DATA_GAS,
                GAS_PRICE,
                GAS_TOKEN.address,
                VERSION
            )
        then(gnosisSafeRepositoryMock).should().loadInfo(SAFE_TRANSACTION.wrapped.address)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(transactionExecutionRepositoryMock).shouldHaveNoMoreInteractions()
        then(cryptoHelper).shouldHaveZeroInteractions()
        then(pushServiceRepositoryMock).shouldHaveZeroInteractions()

        testObserver.assertValue { it is ErrorResult && it.error is IllegalStateException }
    }

    @Test
    fun submitTransactionErrorCalculatingHash() {
        val testObserver = TestObserver.create<Result<Unit>>()
        val exception = Exception()
        given(
            transactionExecutionRepositoryMock.calculateHash(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.error(exception))

        given(gnosisSafeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.just(
            SafeInfo(SAFE_TRANSACTION.wrapped.address, Wei.ZERO, 1, emptyList(), false, emptyList(), VERSION)
        ))

        viewModel.setup(
            SAFE_TRANSACTION,
            SIGNATURE_1,
            SIGNATURE_2,
            TX_GAS,
            DATA_GAS,
            OPERATIONAL_GAS,
            GAS_PRICE,
            GAS_TOKEN.address,
            NEW_AUTHENTICATOR_INFO,
            TX_HASH
        )
        viewModel.submitTransaction().subscribe(testObserver)

        then(transactionExecutionRepositoryMock).should()
            .calculateHash(
                SAFE_TRANSACTION.wrapped.address,
                SAFE_TRANSACTION,
                TX_GAS,
                DATA_GAS,
                GAS_PRICE,
                GAS_TOKEN.address,
                VERSION
            )
        then(gnosisSafeRepositoryMock).should().loadInfo(SAFE_TRANSACTION.wrapped.address)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(transactionExecutionRepositoryMock).shouldHaveNoMoreInteractions()
        then(cryptoHelper).shouldHaveZeroInteractions()
        then(pushServiceRepositoryMock).shouldHaveZeroInteractions()

        testObserver.assertValue { it is ErrorResult && it.error == exception }
    }

    companion object {
        private val SAFE_TRANSACTION = SafeTransaction(
            wrapped = Transaction(
                address = 42.toBigInteger().let { Solidity.Address(it) }
            ),
            operation = TransactionExecutionRepository.Operation.CALL
        )

        private val SIGNATURE_1 = Signature(r = 256.toBigInteger(), s = 257.toBigInteger(), v = 27)
        private val SIGNATURE_2 = Signature(r = 356.toBigInteger(), s = 357.toBigInteger(), v = 27)
        private val TX_GAS = 5000.toBigInteger()
        private val DATA_GAS = 6000.toBigInteger()
        private val OPERATIONAL_GAS = 7000.toBigInteger()
        private val GAS_PRICE = 1000.toBigInteger()
        private val GAS_TOKEN = ERC20Token("0x1337".asEthereumAddress()!!, "Golden Wishing Spheres", "DBZ", 7)
        private val NEW_AUTHENTICATOR_INFO = AuthenticatorSetupInfo(
            "0xbaddad".asEthereumAddress()!!.asOwner(),
            AuthenticatorInfo(AuthenticatorInfo.Type.EXTENSION, 42.toBigInteger().let { Solidity.Address(it) })
        )
        private val VERSION = SemVer(1, 0, 0)
        private val TX_HASH = byteArrayOf(0, 1, 2, 3, 4, 5)
    }
}
