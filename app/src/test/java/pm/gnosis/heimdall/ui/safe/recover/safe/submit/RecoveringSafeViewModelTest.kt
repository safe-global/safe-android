package pm.gnosis.heimdall.ui.safe.recover.safe.submit

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Predicate
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.RecoveringSafe
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.mockGetString
import pm.gnosis.tests.utils.testRecoveringSafe
import pm.gnosis.utils.asBigInteger
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.hexStringToByteArray
import java.math.BigInteger
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
class RecoveringSafeViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var accountsRepoMock: AccountsRepository

    @Mock
    private lateinit var execRepoMock: TransactionExecutionRepository

    @Mock
    private lateinit var safeRepoMock: GnosisSafeRepository

    @Mock
    private lateinit var tokenRepoMock: TokenRepository

    private lateinit var viewModel: RecoveringSafeViewModel

    @Before
    fun setUp() {
        viewModel = RecoveringSafeViewModel(contextMock, accountsRepoMock, execRepoMock, safeRepoMock, tokenRepoMock)
    }

    @Test
    fun checkSafeStateInfoError() {
        val error = IllegalArgumentException()
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.error(error))

        val observer = TestObserver<Pair<RecoveringSafe, RecoveringSafeContract.RecoveryState>>()
        viewModel.checkSafeState(TEST_SAFE).subscribe(observer)
        observer.assertFailure(Predicate { it == error })
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(execRepoMock).shouldHaveZeroInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
        then(tokenRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkSafeStatePending() {
        // transactionHash != null -> submitted
        val safe = testRecoveringSafe(
            TEST_SAFE, TEST_HASH, TEST_SAFE, gasToken = TEST_TOKEN
        )
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))

        val observer = TestObserver<Pair<RecoveringSafe, RecoveringSafeContract.RecoveryState>>()
        viewModel.checkSafeState(TEST_SAFE).subscribe(observer)
        observer.assertResult(safe to RecoveringSafeContract.RecoveryState.PENDING)
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(execRepoMock).shouldHaveZeroInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
        then(tokenRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkSafeStateCreated() {
        // transactionHash == null -> not submitted yet
        val safe = testRecoveringSafe(
            TEST_SAFE, null, TEST_SAFE, gasToken = TEST_TOKEN
        )
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))
        val recoverTx = SafeTransaction(Transaction(TEST_SAFE, data = "", nonce = BigInteger.ZERO), TransactionExecutionRepository.Operation.CALL)
        val info = TransactionExecutionRepository.ExecuteInformation(
            TEST_TX_HASH, recoverTx, TEST_APP, 2, listOf(TEST_APP),
            TEST_TOKEN, BigInteger.TEN, BigInteger.TEN, BigInteger.TEN, BigInteger.ZERO, BigInteger.ZERO
        )
        given(execRepoMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(Single.just(info))

        val observer = TestObserver<Pair<RecoveringSafe, RecoveringSafeContract.RecoveryState>>()
        viewModel.checkSafeState(TEST_SAFE).subscribe(observer)
        observer.assertResult(safe to RecoveringSafeContract.RecoveryState.CREATED)
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(execRepoMock).should().loadExecuteInformation(TEST_SAFE, TEST_TOKEN, recoverTx)
        then(execRepoMock).shouldHaveNoMoreInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
        then(tokenRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkSafeStateFunded() {
        // transactionHash == null -> not submitted yet
        val safe = testRecoveringSafe(
            TEST_SAFE, null, TEST_SAFE, gasToken = TEST_TOKEN
        )
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))
        val recoverTx = SafeTransaction(Transaction(TEST_SAFE, data = "", nonce = BigInteger.ZERO), TransactionExecutionRepository.Operation.CALL)
        val info = TransactionExecutionRepository.ExecuteInformation(
            TEST_TX_HASH, recoverTx, TEST_APP, 2, listOf(TEST_APP),
            TEST_TOKEN, BigInteger.TEN, BigInteger.TEN, BigInteger.ZERO, BigInteger.ZERO, Wei.ether("1").value
        )
        given(execRepoMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(Single.just(info))

        val observer = TestObserver<Pair<RecoveringSafe, RecoveringSafeContract.RecoveryState>>()
        viewModel.checkSafeState(TEST_SAFE).subscribe(observer)
        observer.assertResult(safe to RecoveringSafeContract.RecoveryState.FUNDED)
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(execRepoMock).should().loadExecuteInformation(TEST_SAFE, TEST_TOKEN, recoverTx)
        then(execRepoMock).shouldHaveNoMoreInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
        then(tokenRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkSafeStateTxErrorNonce() {
        // transactionHash == null -> not submitted yet
        val safe = testRecoveringSafe(
            TEST_SAFE, null, TEST_SAFE, gasToken = TEST_TOKEN
        )
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))
        // Nonce changed
        val recoverTx = SafeTransaction(Transaction(TEST_SAFE, data = "", nonce = BigInteger.ONE), TransactionExecutionRepository.Operation.CALL)
        val info = TransactionExecutionRepository.ExecuteInformation(
            TEST_TX_HASH, recoverTx, TEST_APP, 2, listOf(TEST_APP),
            TEST_TOKEN, BigInteger.TEN, BigInteger.TEN, BigInteger.TEN, BigInteger.ZERO, Wei.ether("1").value
        )
        given(execRepoMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(Single.just(info))

        val observer = TestObserver<Pair<RecoveringSafe, RecoveringSafeContract.RecoveryState>>()
        viewModel.checkSafeState(TEST_SAFE).subscribe(observer)
        observer.assertResult(safe to RecoveringSafeContract.RecoveryState.ERROR)
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(execRepoMock).should()
            .loadExecuteInformation(TEST_SAFE, TEST_TOKEN, recoverTx.copy(wrapped = recoverTx.wrapped.copy(nonce = BigInteger.ZERO)))
        then(execRepoMock).shouldHaveNoMoreInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
        then(tokenRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkSafeStateTxErrorValue() {
        // transactionHash == null -> not submitted yet
        val safe = testRecoveringSafe(
            TEST_SAFE, null, TEST_SAFE, gasToken = TEST_TOKEN
        )
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))
        // Value not null
        val recoverTx = SafeTransaction(
            Transaction(TEST_SAFE, data = "", nonce = BigInteger.ZERO, value = Wei.ether("1")),
            TransactionExecutionRepository.Operation.CALL
        )
        val info = TransactionExecutionRepository.ExecuteInformation(
            TEST_TX_HASH, recoverTx, TEST_APP, 2, listOf(TEST_APP),
            TEST_TOKEN, BigInteger.TEN, BigInteger.TEN, BigInteger.ZERO, BigInteger.ZERO, Wei.ether("1").value
        )
        given(execRepoMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(Single.just(info))

        val observer = TestObserver<Pair<RecoveringSafe, RecoveringSafeContract.RecoveryState>>()
        viewModel.checkSafeState(TEST_SAFE).subscribe(observer)
        observer.assertResult(safe to RecoveringSafeContract.RecoveryState.ERROR)
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(execRepoMock).should().loadExecuteInformation(TEST_SAFE, TEST_TOKEN, recoverTx.copy(wrapped = recoverTx.wrapped.copy(value = null)))
        then(execRepoMock).shouldHaveNoMoreInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
        then(tokenRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkSafeStateNetworkError() {
        contextMock.mockGetString()
        // transactionHash == null -> not submitted yet
        val safe = testRecoveringSafe(
            TEST_SAFE, null, TEST_SAFE, gasToken = TEST_TOKEN
        )
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))
        given(execRepoMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(Single.error(UnknownHostException()))

        val observer = TestObserver<Pair<RecoveringSafe, RecoveringSafeContract.RecoveryState>>()
        viewModel.checkSafeState(TEST_SAFE).subscribe(observer)
        observer.assertFailure(Predicate { it == SimpleLocalizedException(R.string.error_check_internet_connection.toString()) })
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        val recoverTx = SafeTransaction(Transaction(TEST_SAFE, data = "", nonce = BigInteger.ZERO), TransactionExecutionRepository.Operation.CALL)
        then(execRepoMock).should().loadExecuteInformation(TEST_SAFE, TEST_TOKEN, recoverTx)
        then(execRepoMock).shouldHaveNoMoreInteractions()
        then(contextMock).should().getString(R.string.error_check_internet_connection)
        then(contextMock).shouldHaveNoMoreInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
        then(tokenRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkRecoveryStatusInfoError() {
        val error = IllegalArgumentException()
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.error(error))

        val observer = TestObserver<Solidity.Address>()
        viewModel.checkRecoveryStatus(TEST_SAFE).subscribe(observer)
        observer.assertFailure(Predicate { it == error })
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(execRepoMock).shouldHaveZeroInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
        then(tokenRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkRecoveryStatusNoTxHash() {
        val safe = testRecoveringSafe(
            TEST_SAFE, null, TEST_SAFE, gasToken = TEST_TOKEN
        )
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))

        val observer = TestObserver<Solidity.Address>()
        viewModel.checkRecoveryStatus(TEST_SAFE).subscribe(observer)
        observer.assertFailure(IllegalStateException::class.java)
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(execRepoMock).shouldHaveZeroInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
        then(tokenRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkRecoveryStatusNetworkError() {
        contextMock.mockGetString()
        val safe = testRecoveringSafe(
            TEST_SAFE, TEST_HASH, TEST_SAFE, gasToken = TEST_TOKEN
        )
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))
        given(execRepoMock.observeTransactionStatus(MockUtils.any())).willReturn(Observable.error(UnknownHostException()))

        val observer = TestObserver<Solidity.Address>()
        viewModel.checkRecoveryStatus(TEST_SAFE).subscribe(observer)
        observer.assertFailure(Predicate { it == SimpleLocalizedException(R.string.error_check_internet_connection.toString()) })
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(execRepoMock).should().observeTransactionStatus(TEST_HASH)
        then(execRepoMock).shouldHaveNoMoreInteractions()
        then(contextMock).should().getString(R.string.error_check_internet_connection)
        then(contextMock).shouldHaveNoMoreInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
        then(tokenRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkRecoveryStatusTxFailed() {
        val safe = testRecoveringSafe(
            TEST_SAFE, TEST_HASH, TEST_SAFE, gasToken = TEST_TOKEN
        )
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))
        given(execRepoMock.observeTransactionStatus(MockUtils.any())).willReturn(Observable.just(false to System.currentTimeMillis()))

        val observer = TestObserver<Solidity.Address>()
        viewModel.checkRecoveryStatus(TEST_SAFE).subscribe(observer)
        observer.assertFailure(IllegalStateException::class.java)
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(execRepoMock).should().observeTransactionStatus(TEST_HASH)
        then(execRepoMock).shouldHaveNoMoreInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
        then(tokenRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkRecoveryStatusToDeployedSafeError() {
        val safe = testRecoveringSafe(
            TEST_SAFE, TEST_HASH, TEST_SAFE, gasToken = TEST_TOKEN
        )
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))
        given(execRepoMock.observeTransactionStatus(MockUtils.any())).willReturn(Observable.just(true to System.currentTimeMillis()))
        given(safeRepoMock.recoveringSafeToDeployedSafe(MockUtils.any())).willReturn(Completable.error(IllegalArgumentException()))

        val observer = TestObserver<Solidity.Address>()
        viewModel.checkRecoveryStatus(TEST_SAFE).subscribe(observer)
        observer.assertFailure(IllegalArgumentException::class.java)
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).should().recoveringSafeToDeployedSafe(safe)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(execRepoMock).should().observeTransactionStatus(TEST_HASH)
        then(execRepoMock).shouldHaveNoMoreInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
        then(tokenRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkRecoveryStatus() {
        val safe = testRecoveringSafe(
            TEST_SAFE, TEST_HASH, TEST_SAFE, gasToken = TEST_TOKEN
        )
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))
        given(execRepoMock.observeTransactionStatus(MockUtils.any())).willReturn(Observable.just(true to System.currentTimeMillis()))
        given(safeRepoMock.recoveringSafeToDeployedSafe(MockUtils.any())).willReturn(Completable.complete())

        val observer = TestObserver<Solidity.Address>()
        viewModel.checkRecoveryStatus(TEST_SAFE).subscribe(observer)
        observer.assertResult(TEST_SAFE)
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).should().recoveringSafeToDeployedSafe(safe)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(execRepoMock).should().observeTransactionStatus(TEST_HASH)
        then(execRepoMock).shouldHaveNoMoreInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
        then(tokenRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun observeRecoveryInfoSafeError() {
        val error = IllegalArgumentException()
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.error(error))

        val observer = TestObserver<Result<RecoveringSafeContract.RecoveryInfo>>()
        viewModel.observeRecoveryInfo(TEST_SAFE).subscribe(observer)
        observer.assertFailure(Predicate { it == error })
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(tokenRepoMock).shouldHaveZeroInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(execRepoMock).shouldHaveZeroInteractions()
        then(accountsRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun observeRecoveryInfoTokenError() {
        contextMock.mockGetString()
        val safe = testRecoveringSafe(
            TEST_SAFE, TEST_HASH, TEST_SAFE,
            txGas = BigInteger.TEN, dataGas = BigInteger.valueOf(100), operationalGas = BigInteger.ONE,
            gasPrice = BigInteger.TEN, gasToken = TEST_TOKEN
        )
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))
        given(tokenRepoMock.loadToken(MockUtils.any())).willReturn(Single.error(UnknownHostException()))

        val observer = TestObserver<Result<RecoveringSafeContract.RecoveryInfo>>()
        viewModel.observeRecoveryInfo(TEST_SAFE).subscribe(observer)
        observer.assertResult(
            DataResult(RecoveringSafeContract.RecoveryInfo(TEST_SAFE_CHECK, null, BigInteger.valueOf(1110))),
            ErrorResult(SimpleLocalizedException(R.string.error_check_internet_connection.toString()))
        )
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(tokenRepoMock).should().loadToken(TEST_TOKEN)
        then(tokenRepoMock).shouldHaveNoMoreInteractions()
        then(contextMock).should().getString(R.string.error_check_internet_connection)
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
        then(execRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun observeRecoveryInfo() {
        val safe = testRecoveringSafe(
            TEST_SAFE, TEST_HASH, TEST_SAFE,
            txGas = BigInteger.ONE, dataGas = BigInteger.TEN, gasPrice = BigInteger.TEN, gasToken = TEST_TOKEN
        )
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))
        given(tokenRepoMock.loadToken(MockUtils.any())).willReturn(Single.just(ERC20Token.ETHER_TOKEN))

        val observer = TestObserver<Result<RecoveringSafeContract.RecoveryInfo>>()
        viewModel.observeRecoveryInfo(TEST_SAFE).subscribe(observer)
        observer.assertResult(
            DataResult(RecoveringSafeContract.RecoveryInfo(TEST_SAFE_CHECK, null, BigInteger.valueOf(110))),
            DataResult(RecoveringSafeContract.RecoveryInfo(TEST_SAFE_CHECK, ERC20Token.ETHER_TOKEN, BigInteger.valueOf(110)))
        )
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(tokenRepoMock).should().loadToken(TEST_TOKEN)
        then(tokenRepoMock).shouldHaveNoMoreInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
        then(execRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun loadRecoveryExecuteInfoSafeError() {
        val error = IllegalArgumentException()
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.error(error))

        val observer = TestObserver<RecoveringSafeContract.RecoveryExecuteInfo>()
        viewModel.loadRecoveryExecuteInfo(TEST_SAFE).subscribe(observer)
        observer.assertFailure(Predicate { it == error })
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(execRepoMock).shouldHaveZeroInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(tokenRepoMock).shouldHaveZeroInteractions()
        then(accountsRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun loadRecoveryExecuteInfoTokenNetworkError() {
        contextMock.mockGetString()
        // transactionHash == null -> not submitted yet
        val safe = testRecoveringSafe(
            TEST_SAFE, null, TEST_SAFE, gasToken = TEST_TOKEN
        )
        given(tokenRepoMock.loadToken(MockUtils.any())).willReturn(Single.error(UnknownHostException()))
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))

        val observer = TestObserver<RecoveringSafeContract.RecoveryExecuteInfo>()
        viewModel.loadRecoveryExecuteInfo(TEST_SAFE).subscribe(observer)
        observer.assertFailure(Predicate { it == SimpleLocalizedException(R.string.error_check_internet_connection.toString()) })
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(contextMock).should().getString(R.string.error_check_internet_connection)
        then(contextMock).shouldHaveZeroInteractions()
        // Load the payment token info
        then(tokenRepoMock).should().loadToken(TEST_TOKEN)
        then(tokenRepoMock).shouldHaveNoMoreInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
        then(execRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun loadRecoveryExecuteInfoNetworkError() {
        contextMock.mockGetString()
        // transactionHash == null -> not submitted yet
        val safe = testRecoveringSafe(
            TEST_SAFE, null, TEST_SAFE, gasToken = TEST_TOKEN
        )
        given(tokenRepoMock.loadToken(MockUtils.any())).willReturn(Single.just(ERC20Token.ETHER_TOKEN))
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))
        given(execRepoMock.loadSafeExecuteState(MockUtils.any(), MockUtils.any())).willReturn(Single.error(UnknownHostException()))

        val observer = TestObserver<RecoveringSafeContract.RecoveryExecuteInfo>()
        viewModel.loadRecoveryExecuteInfo(TEST_SAFE).subscribe(observer)
        observer.assertFailure(Predicate { it == SimpleLocalizedException(R.string.error_check_internet_connection.toString()) })
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(execRepoMock).should().loadSafeExecuteState(TEST_SAFE, TEST_TOKEN)
        then(execRepoMock).shouldHaveNoMoreInteractions()
        then(contextMock).should().getString(R.string.error_check_internet_connection)
        then(contextMock).shouldHaveZeroInteractions()
        // Load the payment token info
        then(tokenRepoMock).should().loadToken(TEST_TOKEN)
        then(tokenRepoMock).shouldHaveNoMoreInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun loadRecoveryExecuteInfo() {
        // transactionHash == null -> not submitted yet
        val safe = testRecoveringSafe(
            TEST_SAFE, null, TEST_SAFE, gasToken = TEST_TOKEN
        )
        given(tokenRepoMock.loadToken(MockUtils.any())).willReturn(Single.just(ERC20Token.ETHER_TOKEN))
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))
        val info = RecoveringSafeContract.RecoveryExecuteInfo(
            BigInteger.TEN,
            safe.gasPrice * (safe.operationalGas + safe.txGas + safe.dataGas),
            ERC20Token.ETHER_TOKEN,
            true
        )
        given(execRepoMock.loadSafeExecuteState(MockUtils.any(), MockUtils.any())).willReturn(
            Single.just(
                TransactionExecutionRepository.SafeExecuteState(
                    TEST_APP,
                    2,
                    listOf(TEST_APP),
                    safe.nonce,
                    info.balance
                )
            )
        )

        val observer = TestObserver<RecoveringSafeContract.RecoveryExecuteInfo>()
        viewModel.loadRecoveryExecuteInfo(TEST_SAFE).subscribe(observer)
        observer.assertResult(info)
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(execRepoMock).should().loadSafeExecuteState(TEST_SAFE, TEST_TOKEN)
        then(execRepoMock).shouldHaveNoMoreInteractions()
        // Load the payment token info
        then(tokenRepoMock).should().loadToken(TEST_TOKEN)
        then(tokenRepoMock).shouldHaveNoMoreInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun submitRecoverySafeError() {
        val error = IllegalArgumentException()
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.error(error))

        val observer = TestObserver<Solidity.Address>()
        viewModel.submitRecovery(TEST_SAFE).subscribe(observer)
        observer.assertFailure(Predicate { it == error })
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(accountsRepoMock).shouldHaveZeroInteractions()
        then(execRepoMock).shouldHaveZeroInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(tokenRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun submitRecoveryHashError() {
        val safe = testRecoveringSafe(
            TEST_SAFE, null, TEST_SAFE,
            txGas = BigInteger.ZERO, dataGas = BigInteger.ONE, gasPrice = BigInteger.TEN, gasToken = TEST_TOKEN
        )
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))
        val error = IllegalArgumentException()
        given(execRepoMock.calculateHash(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.error(error))

        val observer = TestObserver<Solidity.Address>()
        viewModel.submitRecovery(TEST_SAFE).subscribe(observer)
        observer.assertFailure(Predicate { it == error })
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        val recoverTx = SafeTransaction(Transaction(TEST_SAFE, data = "", nonce = BigInteger.ZERO), TransactionExecutionRepository.Operation.CALL)
        then(execRepoMock).should().calculateHash(TEST_SAFE, recoverTx, BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN, TEST_TOKEN)
        then(execRepoMock).shouldHaveNoMoreInteractions()
        then(accountsRepoMock).shouldHaveZeroInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(tokenRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun submitRecoveryRecoverError() {
        val signature1 = Signature(BigInteger.valueOf(11), BigInteger.valueOf(5), 27)
        val signature2 = Signature(BigInteger.valueOf(19), BigInteger.valueOf(89), 28)
        val safe = testRecoveringSafe(
            TEST_SAFE, null, TEST_SAFE,
            txGas = BigInteger.ZERO, dataGas = BigInteger.ONE, gasPrice = BigInteger.TEN, gasToken = TEST_TOKEN,
            signatures = listOf(signature1, signature2)
        )
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))
        given(execRepoMock.calculateHash(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(TEST_HASH_BYTES))
        given(accountsRepoMock.recover(TEST_HASH_BYTES, signature1)).willReturn(Single.just(TEST_RECOVER_1))
        val error = IllegalArgumentException()
        given(accountsRepoMock.recover(TEST_HASH_BYTES, signature2)).willReturn(Single.error(error))

        val observer = TestObserver<Solidity.Address>()
        viewModel.submitRecovery(TEST_SAFE).subscribe(observer)
        observer.assertFailure(Predicate { it == error })
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        val recoverTx = SafeTransaction(Transaction(TEST_SAFE, data = "", nonce = BigInteger.ZERO), TransactionExecutionRepository.Operation.CALL)
        then(execRepoMock).should().calculateHash(TEST_SAFE, recoverTx, BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN, TEST_TOKEN)
        then(execRepoMock).shouldHaveNoMoreInteractions()
        then(accountsRepoMock).should().recover(TEST_HASH_BYTES, signature1)
        then(accountsRepoMock).should().recover(TEST_HASH_BYTES, signature2)
        then(accountsRepoMock).shouldHaveNoMoreInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(tokenRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun submitRecoverySubmitError() {
        contextMock.mockGetString()
        val signature1 = Signature(BigInteger.valueOf(11), BigInteger.valueOf(5), 27)
        val signature2 = Signature(BigInteger.valueOf(19), BigInteger.valueOf(89), 28)
        val safe = testRecoveringSafe(
            TEST_SAFE, null, TEST_SAFE,
            txGas = BigInteger.ZERO, dataGas = BigInteger.ONE, gasPrice = BigInteger.TEN, gasToken = TEST_TOKEN,
            signatures = listOf(signature1, signature2)
        )
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))
        given(execRepoMock.calculateHash(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(TEST_HASH_BYTES))
        given(accountsRepoMock.recover(TEST_HASH_BYTES, signature1)).willReturn(Single.just(TEST_RECOVER_1))
        given(accountsRepoMock.recover(TEST_HASH_BYTES, signature2)).willReturn(Single.just(TEST_RECOVER_2))
        given(
            execRepoMock.submit(
                MockUtils.any(), MockUtils.any(), MockUtils.any(), anyBoolean(),
                MockUtils.any(), MockUtils.any(), MockUtils.any(), anyBoolean()
            )
        ).willReturn(Single.error(ConnectException()))

        val observer = TestObserver<Solidity.Address>()
        viewModel.submitRecovery(TEST_SAFE).subscribe(observer)
        observer.assertFailure(Predicate { it == SimpleLocalizedException(R.string.error_check_internet_connection.toString()) })
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        val recoverTx = SafeTransaction(Transaction(TEST_SAFE, data = "", nonce = BigInteger.ZERO), TransactionExecutionRepository.Operation.CALL)
        then(execRepoMock).should().calculateHash(TEST_SAFE, recoverTx, BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN, TEST_TOKEN)
        then(execRepoMock).should().submit(
            TEST_SAFE, recoverTx, mapOf(TEST_RECOVER_1 to signature1, TEST_RECOVER_2 to signature2),
            false, BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN, false
        )
        then(execRepoMock).shouldHaveNoMoreInteractions()
        then(accountsRepoMock).should().recover(TEST_HASH_BYTES, signature1)
        then(accountsRepoMock).should().recover(TEST_HASH_BYTES, signature2)
        then(accountsRepoMock).shouldHaveNoMoreInteractions()
        then(contextMock).should().getString(R.string.error_check_internet_connection)
        then(contextMock).shouldHaveNoMoreInteractions()

        // Repos are not related to this method
        then(tokenRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun submitRecoveryUpdateInfoError() {
        val signature1 = Signature(BigInteger.valueOf(11), BigInteger.valueOf(5), 27)
        val signature2 = Signature(BigInteger.valueOf(19), BigInteger.valueOf(89), 28)
        val safe = testRecoveringSafe(
            TEST_SAFE, null, TEST_SAFE,
            txGas = BigInteger.ZERO, dataGas = BigInteger.ONE, gasPrice = BigInteger.TEN, gasToken = TEST_TOKEN,
            signatures = listOf(signature1, signature2)
        )
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))
        given(execRepoMock.calculateHash(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(TEST_HASH_BYTES))
        given(accountsRepoMock.recover(TEST_HASH_BYTES, signature1)).willReturn(Single.just(TEST_RECOVER_1))
        given(accountsRepoMock.recover(TEST_HASH_BYTES, signature2)).willReturn(Single.just(TEST_RECOVER_2))
        given(
            execRepoMock.submit(
                MockUtils.any(), MockUtils.any(), MockUtils.any(), anyBoolean(),
                MockUtils.any(), MockUtils.any(), MockUtils.any(), anyBoolean()
            )
        ).willReturn(Single.just(TEST_TX_HASH))
        val error = IllegalArgumentException()
        given(safeRepoMock.updateRecoveringSafe(MockUtils.any())).willReturn(Completable.error(error))

        val observer = TestObserver<Solidity.Address>()
        viewModel.submitRecovery(TEST_SAFE).subscribe(observer)
        observer.assertFailure(Predicate { it == error })
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).should().updateRecoveringSafe(safe.copy(transactionHash = TEST_TX_HASH.hexAsBigInteger()))
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        val recoverTx = SafeTransaction(Transaction(TEST_SAFE, data = "", nonce = BigInteger.ZERO), TransactionExecutionRepository.Operation.CALL)
        then(execRepoMock).should().calculateHash(TEST_SAFE, recoverTx, BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN, TEST_TOKEN)
        then(execRepoMock).should().submit(
            TEST_SAFE, recoverTx, mapOf(TEST_RECOVER_1 to signature1, TEST_RECOVER_2 to signature2),
            false, BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN, false
        )
        then(execRepoMock).shouldHaveNoMoreInteractions()
        then(accountsRepoMock).should().recover(TEST_HASH_BYTES, signature1)
        then(accountsRepoMock).should().recover(TEST_HASH_BYTES, signature2)
        then(accountsRepoMock).shouldHaveNoMoreInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(tokenRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun submitRecovery() {
        val signature1 = Signature(BigInteger.valueOf(11), BigInteger.valueOf(5), 27)
        val signature2 = Signature(BigInteger.valueOf(19), BigInteger.valueOf(89), 28)
        val safe = testRecoveringSafe(
            TEST_SAFE, null, TEST_SAFE,
            txGas = BigInteger.ZERO, dataGas = BigInteger.ONE, gasPrice = BigInteger.TEN, gasToken = TEST_TOKEN,
            signatures = listOf(signature1, signature2)
        )
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))
        given(execRepoMock.calculateHash(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(TEST_HASH_BYTES))
        given(accountsRepoMock.recover(TEST_HASH_BYTES, signature1)).willReturn(Single.just(TEST_RECOVER_1))
        given(accountsRepoMock.recover(TEST_HASH_BYTES, signature2)).willReturn(Single.just(TEST_RECOVER_2))
        given(
            execRepoMock.submit(
                MockUtils.any(), MockUtils.any(), MockUtils.any(), anyBoolean(),
                MockUtils.any(), MockUtils.any(), MockUtils.any(), anyBoolean()
            )
        ).willReturn(Single.just(TEST_TX_HASH))
        given(safeRepoMock.updateRecoveringSafe(MockUtils.any())).willReturn(Completable.complete())

        val observer = TestObserver<Solidity.Address>()
        viewModel.submitRecovery(TEST_SAFE).subscribe(observer)
        observer.assertResult(TEST_SAFE)
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).should().updateRecoveringSafe(safe.copy(transactionHash = TEST_TX_HASH.hexAsBigInteger()))
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        val recoverTx = SafeTransaction(Transaction(TEST_SAFE, data = "", nonce = BigInteger.ZERO), TransactionExecutionRepository.Operation.CALL)
        then(execRepoMock).should().calculateHash(TEST_SAFE, recoverTx, BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN, TEST_TOKEN)
        then(execRepoMock).should().submit(
            TEST_SAFE, recoverTx, mapOf(TEST_RECOVER_1 to signature1, TEST_RECOVER_2 to signature2),
            false, BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN, false
        )
        then(execRepoMock).shouldHaveNoMoreInteractions()
        then(accountsRepoMock).should().recover(TEST_HASH_BYTES, signature1)
        then(accountsRepoMock).should().recover(TEST_HASH_BYTES, signature2)
        then(accountsRepoMock).shouldHaveNoMoreInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(tokenRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkRecoveryFundedSafeError() {
        val error = IllegalArgumentException()
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.error(error))

        val observer = TestObserver<Solidity.Address>()
        viewModel.checkRecoveryFunded(TEST_SAFE).subscribe(observer)
        observer.assertFailure(Predicate { it == error })
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(tokenRepoMock).shouldHaveZeroInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
        then(execRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkRecoveryFunded() {
        val testScheduler = TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { _ -> testScheduler }

        var returnedBalance: BigInteger? = null
        val safe = testRecoveringSafe(
            TEST_SAFE, null, TEST_SAFE,
            txGas = BigInteger.ZERO, dataGas = BigInteger.ONE, gasPrice = BigInteger.TEN, gasToken = TEST_TOKEN
        )
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(safe))
        given(tokenRepoMock.loadTokenBalances(MockUtils.any(), MockUtils.any())).willReturn(Observable.fromCallable {
            returnedBalance?.let { listOf(ERC20Token(TEST_TOKEN, decimals = 0, name = "", symbol = "") to it) } ?: throw UnknownHostException()
        })

        val observer = TestObserver<Solidity.Address>()
        viewModel.checkRecoveryFunded(TEST_SAFE).subscribe(observer)

        observer.assertEmpty()
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        observer.assertEmpty()

        returnedBalance = BigInteger.ZERO
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        observer.assertEmpty()

        returnedBalance = BigInteger.TEN
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        observer.assertResult(TEST_SAFE)

        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(tokenRepoMock).should().loadTokenBalances(TEST_SAFE, listOf(ERC20Token(TEST_TOKEN, decimals = 0, name = "", symbol = "")))
        then(tokenRepoMock).shouldHaveNoMoreInteractions()
        // No error message mapping
        then(contextMock).shouldHaveZeroInteractions()

        // Repos are not related to this method
        then(accountsRepoMock).shouldHaveZeroInteractions()
        then(execRepoMock).shouldHaveZeroInteractions()
    }


    companion object {
        private const val TEST_TX_HASH = "0x4c6ddf3c4d48fd987762c1136cb33b30b00c05b1714a127b8abdde53d0f45de1"
        private const val TEST_SAFE_CHECK = "0x1f81FFF89Bd57811983a35650296681f99C65C7E"
        private val TEST_APP = "0x71De9579cD3857ce70058a1ce19e3d8894f65Ab9".asEthereumAddress()!!
        private val TEST_HASH_BYTES = Sha3Utils.keccak("France is world champion".toByteArray())
        private val TEST_HASH = TEST_HASH_BYTES.asBigInteger()
        private val TEST_TOKEN = "0x0".asEthereumAddress()!!
        private val TEST_RECOVER_1 = "0x979861dF79C7408553aAF20c01Cfb3f81CCf9341".asEthereumAddress()!!
        private val TEST_RECOVER_2 = "0x8e6A5aDb2B88257A3DAc7A76A7B4EcaCdA090b66".asEthereumAddress()!!
        private val TEST_SAFE = TEST_SAFE_CHECK.asEthereumAddress()!!
    }
}
