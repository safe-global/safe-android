package pm.gnosis.heimdall.ui.transactions

import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.models.Transaction
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import java.math.BigInteger


@RunWith(MockitoJUnitRunner::class)
class ViewTransactionViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var transactionDetailsRepositoryMock: TransactionDetailsRepository

    private lateinit var viewModel: ViewTransactionViewModel

    @Before
    fun setUp() {
        viewModel = ViewTransactionViewModel(transactionDetailsRepositoryMock)
    }

    private fun testCheckTransactionType(transaction: Transaction, expectedType: TransactionType) {
        val testObserver = TestObserver<TransactionType>()
        viewModel.checkTransactionType(transaction).subscribe(testObserver)
        testObserver.assertNoErrors().assertValue(expectedType).assertComplete()
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

}