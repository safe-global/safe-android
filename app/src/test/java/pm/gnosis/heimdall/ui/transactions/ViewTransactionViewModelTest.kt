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
    private fun testTransactionInfo(info: TransactionRepository.TransactionInfo, estimateResult: Result<Wei>,
                                    expectedType: TransactionRepository.SubmitType?, vararg expectedResults: Result<Info>) {
        given(transactionRepositoryMock.loadInformation(TEST_SAFE, TEST_TRANSACTION)).willReturn(Single.just(info))

        val estimateReturn = when (estimateResult) {
            is DataResult -> Single.just(estimateResult.data)
            is ErrorResult -> Single.error(estimateResult.error)
        }
        expectedType?.let {
            given(transactionRepositoryMock.estimateFees(TEST_SAFE, TEST_TRANSACTION, it)).willReturn(estimateReturn)
        }

        val testObserver = TestObserver<Result<Info>>()
        viewModel.loadTransactionInfo(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)

        testObserver.assertNoErrors()
                .assertValues(*expectedResults)
                .assertComplete()

        then(transactionRepositoryMock).should().loadInformation(TEST_SAFE, TEST_TRANSACTION)
        expectedType?.let {
            then(transactionRepositoryMock).should().estimateFees(TEST_SAFE, TEST_TRANSACTION, it)
        }
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        BDDMockito.reset(transactionRepositoryMock)
    }

    @Test
    fun loadTransactionInfo() {
        // Test confirm and execute
        val confirmAndExecute = TransactionRepository.TransactionInfo(true, 1, 0, false, false)
        testTransactionInfo(confirmAndExecute, DataResult(TEST_TRANSACTION_FEES),
                TransactionRepository.SubmitType.CONFIRM_AND_EXECUTE, DataResult(Info(confirmAndExecute)), DataResult(Info(confirmAndExecute, TEST_TRANSACTION_FEES)))

        // Test confirm
        val confirm = TransactionRepository.TransactionInfo(true, 2, 0, false, false)
        testTransactionInfo(confirm, DataResult(TEST_TRANSACTION_FEES),
                TransactionRepository.SubmitType.CONFIRM, DataResult(Info(confirm)), DataResult(Info(confirm, TEST_TRANSACTION_FEES)))

        // Test execute
        val execute = TransactionRepository.TransactionInfo(false, 2, 2, false, false)
        testTransactionInfo(execute, DataResult(TEST_TRANSACTION_FEES),
                TransactionRepository.SubmitType.EXECUTE, DataResult(Info(execute)), DataResult(Info(execute, TEST_TRANSACTION_FEES)))

        // Test already executed
        val executed = TransactionRepository.TransactionInfo(false, 2, 2, true, false)
        testTransactionInfo(executed, DataResult(TEST_TRANSACTION_FEES),
                null, DataResult(Info(executed)), ErrorResult(SimpleLocalizedException(R.string.error_transaction_already_executed.toString())))

        // Test not owner
        val notOwner = TransactionRepository.TransactionInfo(false, 2, 0, false, true)
        testTransactionInfo(notOwner, DataResult(TEST_TRANSACTION_FEES),
                null, DataResult(Info(notOwner)), ErrorResult(SimpleLocalizedException(R.string.error_confirm_not_owner.toString())))

        // Test already confirmed
        val confirmed = TransactionRepository.TransactionInfo(true, 2, 0, false, true)
        testTransactionInfo(confirmed, DataResult(TEST_TRANSACTION_FEES),
                null, DataResult(Info(confirmed)), ErrorResult(SimpleLocalizedException(R.string.error_transaction_already_confirmed.toString())))

        // Test error loading estimate
        val estimateError = TransactionRepository.TransactionInfo(true, 2, 2, false, true)
        val error = IllegalStateException()
        testTransactionInfo(estimateError, ErrorResult(error),
                TransactionRepository.SubmitType.EXECUTE, DataResult(Info(estimateError)), ErrorResult(error))
    }

    @Test
    fun loadTransactionInfoError() {
        val error = IllegalStateException()
        given(transactionRepositoryMock.loadInformation(TEST_SAFE, TEST_TRANSACTION)).willReturn(Single.error(error))

        val testObserver = TestObserver<Result<Info>>()
        viewModel.loadTransactionInfo(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)

        testObserver.assertNoErrors()
                .assertValue(ErrorResult(error))
                .assertComplete()
    }

    private fun testSubmitTransaction(info: TransactionRepository.TransactionInfo, submitError: Throwable?,
                                      expectedType: TransactionRepository.SubmitType?, vararg expectedResults: Result<Unit>) {
        given(transactionRepositoryMock.loadInformation(TEST_SAFE, TEST_TRANSACTION)).willReturn(Single.just(info))

        val submitReturn = submitError?.let { Completable.error(it) } ?: Completable.complete()
        expectedType?.let {
            given(transactionRepositoryMock.submit(TEST_SAFE, TEST_TRANSACTION, it)).willReturn(submitReturn)
        }

        val testObserver = TestObserver<Result<Unit>>()
        viewModel.submitTransaction(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)

        testObserver.assertNoErrors()
                .assertValues(*expectedResults)
                .assertComplete()

        then(transactionRepositoryMock).should().loadInformation(TEST_SAFE, TEST_TRANSACTION)
        expectedType?.let {
            then(transactionRepositoryMock).should().submit(TEST_SAFE, TEST_TRANSACTION, it)
        }
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        BDDMockito.reset(transactionRepositoryMock)
    }

    @Test
    fun submitTransaction() {
        // Test confirm and execute
        val confirmAndExecute = TransactionRepository.TransactionInfo(true, 1, 0, false, false)
        testSubmitTransaction(confirmAndExecute, null,
                TransactionRepository.SubmitType.CONFIRM_AND_EXECUTE, DataResult(Unit))

        // Test confirm
        val confirm = TransactionRepository.TransactionInfo(true, 2, 0, false, false)
        testSubmitTransaction(confirm, null,
                TransactionRepository.SubmitType.CONFIRM, DataResult(Unit))

        // Test execute
        val execute = TransactionRepository.TransactionInfo(false, 2, 2, false, false)
        testSubmitTransaction(execute, null,
                TransactionRepository.SubmitType.EXECUTE, DataResult(Unit))

        // Test already executed
        val executed = TransactionRepository.TransactionInfo(false, 2, 2, true, false)
        testSubmitTransaction(executed, null,
                null, ErrorResult(SimpleLocalizedException(R.string.error_transaction_already_executed.toString())))

        // Test not owner
        val notOwner = TransactionRepository.TransactionInfo(false, 2, 0, false, true)
        testSubmitTransaction(notOwner, null,
                null, ErrorResult(SimpleLocalizedException(R.string.error_confirm_not_owner.toString())))

        // Test already confirmed
        val confirmed = TransactionRepository.TransactionInfo(true, 2, 0, false, true)
        testSubmitTransaction(confirmed, null,
                null, ErrorResult(SimpleLocalizedException(R.string.error_transaction_already_confirmed.toString())))

        // Test error submitting transaction
        val estimateError = TransactionRepository.TransactionInfo(true, 2, 2, false, true)
        val error = IllegalStateException()
        testSubmitTransaction(estimateError, error,
                TransactionRepository.SubmitType.EXECUTE, ErrorResult(error))
    }

    @Test
    fun submitTransactionLoadInfoError() {
        val error = IllegalStateException()
        given(transactionRepositoryMock.loadInformation(TEST_SAFE, TEST_TRANSACTION)).willReturn(Single.error(error))

        val testObserver = TestObserver<Result<Unit>>()
        viewModel.submitTransaction(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)

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
        private val TEST_TRANSACTION = Transaction(BigInteger.ZERO)
        private val TEST_TRANSACTION_FEES = Wei(BigInteger.valueOf(1337))
    }

}