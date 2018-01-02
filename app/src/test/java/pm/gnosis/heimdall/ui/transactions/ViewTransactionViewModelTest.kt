package pm.gnosis.heimdall.ui.transactions

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.data.repositories.models.GasEstimate
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.transactions.ViewTransactionContract.Info
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.mockGetString
import java.math.BigInteger


@RunWith(MockitoJUnitRunner::class)
class ViewTransactionViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var transactionRepositoryMock: TransactionRepository

    @Mock
    lateinit var transactionDetailsRepositoryMock: TransactionDetailsRepository

    private lateinit var viewModel: ViewTransactionViewModel

    @Before
    fun setUp() {
        contextMock.mockGetString()
        viewModel = ViewTransactionViewModel(contextMock, transactionRepositoryMock, transactionDetailsRepositoryMock)
    }

    private fun testTransactionInfo(info: TransactionRepository.TransactionStatus, estimateResult: Result<GasEstimate>,
                                    expectingEstimate: Boolean, vararg expectedResults: Result<Info>) {
        given(transactionRepositoryMock.loadStatus(TEST_SAFE)).willReturn(Single.just(info))

        val estimateReturn = when (estimateResult) {
            is DataResult -> Single.just(estimateResult.data)
            is ErrorResult -> Single.error(estimateResult.error)
        }
        if (expectingEstimate) {
            given(transactionRepositoryMock.estimateFees(TEST_SAFE, TEST_TRANSACTION)).willReturn(estimateReturn)
        }

        val testObserver = TestObserver<Result<Info>>()
        viewModel.loadTransactionInfo(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)

        testObserver.assertNoErrors()
                .assertValues(*expectedResults)
                .assertComplete()

        then(transactionRepositoryMock).should().loadStatus(TEST_SAFE)
        if (expectingEstimate) {
            then(transactionRepositoryMock).should().estimateFees(TEST_SAFE, TEST_TRANSACTION)
        }
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        BDDMockito.reset(transactionRepositoryMock)
    }

    private fun buildInfo(transactionStatus: TransactionRepository.TransactionStatus, estimate: GasEstimate? = null) =
            Info(TEST_SAFE, TEST_TRANSACTION, transactionStatus, estimate)

    @Test
    fun loadTransactionInfo() {

        // Test execute
        val execute = TransactionRepository.TransactionStatus(true, 1, BigInteger.ZERO)
        testTransactionInfo(execute, DataResult(TEST_TRANSACTION_FEES), true,
                DataResult(buildInfo(execute)), DataResult(buildInfo(execute, TEST_TRANSACTION_FEES)))

        // Test not owner
        val notOwner = TransactionRepository.TransactionStatus(false, 1, BigInteger.ZERO)
        testTransactionInfo(notOwner, DataResult(TEST_TRANSACTION_FEES),
                false, DataResult(buildInfo(notOwner)), ErrorResult(SimpleLocalizedException(R.string.error_not_enough_confirmations.toString())))

        // Test not owner with multiple confirms
        val notOwnerMultipleConfirms = TransactionRepository.TransactionStatus(false, 2, BigInteger.ZERO)
        testTransactionInfo(notOwnerMultipleConfirms, DataResult(TEST_TRANSACTION_FEES),
                false, DataResult(buildInfo(notOwnerMultipleConfirms)), ErrorResult(SimpleLocalizedException(R.string.error_not_enough_confirmations.toString())))

        // Test error loading estimate
        val estimateError = TransactionRepository.TransactionStatus(true, 1, BigInteger.ZERO)
        val error = IllegalStateException()
        testTransactionInfo(estimateError, ErrorResult(error),
                true, DataResult(buildInfo(estimateError)), ErrorResult(error))
    }

    @Test
    fun loadTransactionInfoError() {
        val error = IllegalStateException()
        given(transactionRepositoryMock.loadStatus(TEST_SAFE)).willReturn(Single.error(error))

        val testObserver = TestObserver<Result<Info>>()
        viewModel.loadTransactionInfo(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)

        testObserver.assertNoErrors()
                .assertValue(ErrorResult(error))
                .assertComplete()
    }

    private fun testSubmitTransactionWithGas(info: TransactionRepository.TransactionStatus, submitError: Throwable?,
                                             expectingSubmit: Boolean, gasOverride: Wei?,
                                             vararg expectedResults: Result<BigInteger>) {

        given(transactionRepositoryMock.loadStatus(TEST_SAFE)).willReturn(Single.just(info))

        if (expectingSubmit) {
            val submitReturn = submitError?.let { Completable.error(it) } ?: Completable.complete()
            given(transactionRepositoryMock.submit(TEST_SAFE, TEST_TRANSACTION, gasOverride)).willReturn(submitReturn)
        }

        val testObserverDefaultGas = TestObserver<Result<BigInteger>>()
        viewModel.submitTransaction(TEST_SAFE, TEST_TRANSACTION, gasOverride).subscribe(testObserverDefaultGas)

        testObserverDefaultGas.assertNoErrors()
                .assertValues(*expectedResults)
                .assertComplete()

        then(transactionRepositoryMock).should().loadStatus(TEST_SAFE)
        if (expectingSubmit) {
            then(transactionRepositoryMock).should().submit(TEST_SAFE, TEST_TRANSACTION, gasOverride)
        }
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        BDDMockito.reset(transactionRepositoryMock)
    }

    private fun testSubmitTransaction(info: TransactionRepository.TransactionStatus, submitError: Throwable?,
                                      expectingSubmit: Boolean, vararg expectedResults: Result<BigInteger>) {
        testSubmitTransactionWithGas(info, submitError, expectingSubmit, null, *expectedResults)
        testSubmitTransactionWithGas(info, submitError, expectingSubmit, TEST_GAS_OVERRIDE, *expectedResults)
    }

    @Test
    fun submitTransaction() {

        // Test execute
        val execute = TransactionRepository.TransactionStatus(true, 1, BigInteger.ZERO)
        testSubmitTransaction(execute, null, true, DataResult(TEST_SAFE))

        // Test not owner
        val notOwner = TransactionRepository.TransactionStatus(false, 1, BigInteger.ZERO)
        testSubmitTransaction(notOwner, null,
                false, ErrorResult(SimpleLocalizedException(R.string.error_not_enough_confirmations.toString())))

        // Test not owner with multiple confirms
        val notOwnerMultipleConfirms = TransactionRepository.TransactionStatus(false, 2, BigInteger.ZERO)
        testSubmitTransaction(notOwnerMultipleConfirms, null,
                false, ErrorResult(SimpleLocalizedException(R.string.error_not_enough_confirmations.toString())))

        // Test error submitting transaction
        val estimateError = TransactionRepository.TransactionStatus(true, 1, BigInteger.ZERO)
        val error = IllegalStateException()
        testSubmitTransaction(estimateError, error, true, ErrorResult(error))
    }

    @Test
    fun submitTransactionLoadInfoError() {
        val error = IllegalStateException()
        given(transactionRepositoryMock.loadStatus(TEST_SAFE)).willReturn(Single.error(error))

        val testObserver = TestObserver<Result<BigInteger>>()
        viewModel.submitTransaction(TEST_SAFE, TEST_TRANSACTION, null).subscribe(testObserver)

        testObserver.assertNoErrors()
                .assertValue(ErrorResult(error))
                .assertComplete()
    }

    @Test
    fun checkTransactionType() {
        given(transactionDetailsRepositoryMock.loadTransactionType(MockUtils.any()))
                .willReturn(Single.just(TransactionType.GENERIC))

        val testObserver = TestObserver<TransactionType>()
        val transaction = Transaction(BigInteger.ZERO)
        viewModel.checkTransactionType(transaction).subscribe(testObserver)
        testObserver.assertNoErrors().assertValue(TransactionType.GENERIC).assertComplete()
        then(transactionDetailsRepositoryMock).should().loadTransactionType(transaction)
        then(transactionDetailsRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun checkTransactionTypeError() {
        val error = IllegalStateException()
        given(transactionDetailsRepositoryMock.loadTransactionType(MockUtils.any()))
                .willReturn(Single.error(error))

        val testObserver = TestObserver<TransactionType>()
        val transaction = Transaction(BigInteger.ZERO)
        viewModel.checkTransactionType(transaction).subscribe(testObserver)
        testObserver.assertError(error).assertNoValues().assertTerminated()
        then(transactionDetailsRepositoryMock).should().loadTransactionType(transaction)
        then(transactionDetailsRepositoryMock).shouldHaveNoMoreInteractions()
    }

    companion object {
        private val TEST_SAFE = BigInteger.ZERO
        private val TEST_TRANSACTION = Transaction(BigInteger.ZERO, nonce = BigInteger.TEN)
        private val TEST_TRANSACTION_FEES = GasEstimate(BigInteger.valueOf(1337), Wei(BigInteger.valueOf(23)))
        private val TEST_GAS_OVERRIDE = Wei(BigInteger.valueOf(7331))
    }

}
