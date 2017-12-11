package pm.gnosis.heimdall.ui.transactions

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.data.repositories.TransactionRepository.SubmitType.*
import pm.gnosis.heimdall.data.repositories.TransactionRepository.TransactionInfo
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.transactions.BaseTransactionContract.Info
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.mockGetString
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class BaseTransactionViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var transactionRepositoryMock: TransactionRepository

    private lateinit var viewModel: BaseTransactionViewModel

    @Before
    fun setUp() {
        contextMock.mockGetString()
        viewModel = BaseTransactionViewModel(contextMock, transactionRepositoryMock)
    }

    private fun testTransactionInfo(info: TransactionInfo, estimateResult: Result<Wei>,
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

        verify(transactionRepositoryMock).loadInformation(TEST_SAFE, TEST_TRANSACTION)
        expectedType?.let {
            verify(transactionRepositoryMock).estimateFees(TEST_SAFE, TEST_TRANSACTION, it)
        }
        verifyNoMoreInteractions(transactionRepositoryMock)
        reset(transactionRepositoryMock)
    }

    @Test
    fun loadTransactionInfo() {
        // Test confirm and execute
        val confirmAndExecute = TransactionInfo(true, 1, 0, false, false)
        testTransactionInfo(confirmAndExecute, DataResult(TEST_TRANSACTION_FEES),
                CONFIRM_AND_EXECUTE, DataResult(Info(confirmAndExecute)), DataResult(Info(confirmAndExecute, TEST_TRANSACTION_FEES)))

        // Test confirm
        val confirm = TransactionInfo(true, 2, 0, false, false)
        testTransactionInfo(confirm, DataResult(TEST_TRANSACTION_FEES),
                CONFIRM, DataResult(Info(confirm)), DataResult(Info(confirm, TEST_TRANSACTION_FEES)))

        // Test execute
        val execute = TransactionInfo(false, 2, 2, false, false)
        testTransactionInfo(execute, DataResult(TEST_TRANSACTION_FEES),
                EXECUTE, DataResult(Info(execute)), DataResult(Info(execute, TEST_TRANSACTION_FEES)))

        // Test already executed
        val executed = TransactionInfo(false, 2, 2, true, false)
        testTransactionInfo(executed, DataResult(TEST_TRANSACTION_FEES),
                null, DataResult(Info(executed)), ErrorResult(SimpleLocalizedException(R.string.error_transaction_already_executed.toString())))

        // Test not owner
        val notOwner = TransactionInfo(false, 2, 0, false, true)
        testTransactionInfo(notOwner, DataResult(TEST_TRANSACTION_FEES),
                null, DataResult(Info(notOwner)), ErrorResult(SimpleLocalizedException(R.string.error_confirm_not_owner.toString())))

        // Test already confirmed
        val confirmed = TransactionInfo(true, 2, 0, false, true)
        testTransactionInfo(confirmed, DataResult(TEST_TRANSACTION_FEES),
                null, DataResult(Info(confirmed)), ErrorResult(SimpleLocalizedException(R.string.error_transaction_already_confirmed.toString())))

        // Test error loading estimate
        val estimateError = TransactionInfo(true, 2, 2, false, true)
        val error = IllegalStateException()
        testTransactionInfo(estimateError, ErrorResult(error),
                EXECUTE, DataResult(Info(estimateError)), ErrorResult(error))
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

    private fun testSubmitTransaction(info: TransactionInfo, submitError: Throwable?,
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

        verify(transactionRepositoryMock).loadInformation(TEST_SAFE, TEST_TRANSACTION)
        expectedType?.let {
            verify(transactionRepositoryMock).submit(TEST_SAFE, TEST_TRANSACTION, it)
        }
        verifyNoMoreInteractions(transactionRepositoryMock)
        reset(transactionRepositoryMock)
    }

    @Test
    fun submitTransaction() {
        // Test confirm and execute
        val confirmAndExecute = TransactionInfo(true, 1, 0, false, false)
        testSubmitTransaction(confirmAndExecute, null,
                CONFIRM_AND_EXECUTE, DataResult(Unit))

        // Test confirm
        val confirm = TransactionInfo(true, 2, 0, false, false)
        testSubmitTransaction(confirm, null,
                CONFIRM, DataResult(Unit))

        // Test execute
        val execute = TransactionInfo(false, 2, 2, false, false)
        testSubmitTransaction(execute, null,
                EXECUTE, DataResult(Unit))

        // Test already executed
        val executed = TransactionInfo(false, 2, 2, true, false)
        testSubmitTransaction(executed, null,
                null, ErrorResult(SimpleLocalizedException(R.string.error_transaction_already_executed.toString())))

        // Test not owner
        val notOwner = TransactionInfo(false, 2, 0, false, true)
        testSubmitTransaction(notOwner, null,
                null, ErrorResult(SimpleLocalizedException(R.string.error_confirm_not_owner.toString())))

        // Test already confirmed
        val confirmed = TransactionInfo(true, 2, 0, false, true)
        testSubmitTransaction(confirmed, null,
                null, ErrorResult(SimpleLocalizedException(R.string.error_transaction_already_confirmed.toString())))

        // Test error submitting transaction
        val estimateError = TransactionInfo(true, 2, 2, false, true)
        val error = IllegalStateException()
        testSubmitTransaction(estimateError, error,
                EXECUTE, ErrorResult(error))
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

    companion object {
        private val TEST_SAFE = BigInteger.ZERO
        private val TEST_TRANSACTION = Transaction(BigInteger.ZERO)
        private val TEST_TRANSACTION_FEES = Wei(BigInteger.valueOf(1337))
    }

}