package pm.gnosis.heimdall.ui.safe.selection

import android.content.Context
import android.content.Intent
import io.reactivex.Single
import io.reactivex.functions.Predicate
import io.reactivex.observers.TestObserver
import io.reactivex.processors.PublishProcessor
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.mockGetString
import java.math.BigInteger
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class SelectSafeViewModelTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var safeRepositoryMock: GnosisSafeRepository

    @Mock
    private lateinit var detailsRepositoryMock: TransactionDetailsRepository

    private lateinit var viewModel: SelectSafeViewModel

    @Before
    fun setUp() {
        viewModel = SelectSafeViewModel(contextMock, safeRepositoryMock, detailsRepositoryMock)
    }

    @Test
    fun loadSafes() {
        val testProcessor = PublishProcessor.create<List<Safe>>()
        given(safeRepositoryMock.observeDeployedSafes()).willReturn(testProcessor)

        val testObserver = TestObserver<List<Safe>>()
        viewModel.loadSafes().subscribe(testObserver)
        testObserver.assertEmpty()

        val safes = listOf(Safe(Solidity.Address(BigInteger.TEN)))
        testProcessor.offer(safes)
        testObserver.assertResult(safes)

        testProcessor.offer(emptyList())
        // No new results
        testObserver.assertResult(safes)

        then(safeRepositoryMock).should().observeDeployedSafes()
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadSafesEmpty() {
        val testProcessor = PublishProcessor.create<List<Safe>>()
        given(safeRepositoryMock.observeDeployedSafes()).willReturn(testProcessor)

        val testObserver = TestObserver<List<Safe>>()
        viewModel.loadSafes().subscribe(testObserver)
        testObserver.assertEmpty()

        testProcessor.onComplete()
        testObserver.assertFailure(NoSuchElementException::class.java)

        then(safeRepositoryMock).should().observeDeployedSafes()
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadSafesError() {
        val testProcessor = PublishProcessor.create<List<Safe>>()
        given(safeRepositoryMock.observeDeployedSafes()).willReturn(testProcessor)

        val testObserver = TestObserver<List<Safe>>()
        viewModel.loadSafes().subscribe(testObserver)
        testObserver.assertEmpty()

        val error = IllegalStateException()
        testProcessor.onError(error)
        testObserver.assertFailure(Predicate { it == error })

        then(safeRepositoryMock).should().observeDeployedSafes()
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun reviewTransactionNoSafe() {
        contextMock.mockGetString()
        given(detailsRepositoryMock.loadTransactionType(MockUtils.any())).willReturn(Single.just(TransactionType.ETHER_TRANSFER))

        val testObserver = TestObserver<Result<Intent>>()
        viewModel.reviewTransaction(null, TEST_TRANSACTION).subscribe(testObserver)

        testObserver.assertResult(ErrorResult(SimpleLocalizedException(R.string.no_safe_selected_error.toString())))

        then(detailsRepositoryMock).should().loadTransactionType(TEST_TRANSACTION.wrapped)
        then(detailsRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun reviewTransactionError() {
        val error = IllegalStateException()
        given(detailsRepositoryMock.loadTransactionType(MockUtils.any())).willReturn(Single.error(error))

        val testObserver = TestObserver<Result<Intent>>()
        viewModel.reviewTransaction(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)

        testObserver.assertResult(ErrorResult(error))

        then(detailsRepositoryMock).should().loadTransactionType(TEST_TRANSACTION.wrapped)
        then(detailsRepositoryMock).shouldHaveNoMoreInteractions()
    }

    private fun testReviewTransaction(type: TransactionType) {
        given(detailsRepositoryMock.loadTransactionType(MockUtils.any())).willReturn(Single.just(type))

        val testObserver = TestObserver<Result<Intent>>()
        viewModel.reviewTransaction(TEST_SAFE, TEST_TRANSACTION).subscribe(testObserver)

        testObserver.assertValue({ it is DataResult }).assertComplete()

        then(detailsRepositoryMock).should().loadTransactionType(TEST_TRANSACTION.wrapped)
        then(detailsRepositoryMock).shouldHaveNoMoreInteractions()
        Mockito.reset(detailsRepositoryMock)
    }

    @Test
    fun reviewTransaction() {
        TransactionType.values().forEach {
            testReviewTransaction(it)
        }
    }

    companion object {
        private val TEST_SAFE = Solidity.Address(BigInteger.ONE)
        private val TEST_TRANSACTION = SafeTransaction(Transaction(Solidity.Address(BigInteger.TEN)), TransactionExecutionRepository.Operation.CALL)
    }
}
