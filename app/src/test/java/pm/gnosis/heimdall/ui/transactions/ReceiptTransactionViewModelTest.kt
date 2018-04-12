package pm.gnosis.heimdall.ui.transactions

import io.reactivex.Single
import io.reactivex.functions.Predicate
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.TransactionDetails
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class ReceiptTransactionViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private
    lateinit var transactionRepository: TransactionRepository

    @Mock
    private lateinit var transactionDetailsRepository: TransactionDetailsRepository

    private lateinit var viewModel: ReceiptTransactionViewModel

    @Before
    fun setUp() {
        viewModel = ReceiptTransactionViewModel(transactionRepository, transactionDetailsRepository)
    }

    @Test
    fun loadTransactionDetails() {
        val details = TransactionDetails(
            TEST_ID,
            TransactionType.ETHER_TRANSFER,
            null,
            Transaction(Solidity.Address(BigInteger.TEN)),
            Solidity.Address(BigInteger.ONE),
            13
        )
        given(transactionDetailsRepository.loadTransactionDetails(TEST_ID))
            .willReturn(Single.just(details))

        val testObserver = TestObserver<TransactionDetails>()
        viewModel.loadTransactionDetails(TEST_ID).subscribe(testObserver)
        testObserver.assertResult(details)
        then(transactionDetailsRepository).should().loadTransactionDetails(TEST_ID)
        then(transactionDetailsRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadTransactionDetailsError() {
        val error = IllegalStateException()
        given(transactionDetailsRepository.loadTransactionDetails(TEST_ID))
            .willReturn(Single.error(error))

        val testObserver = TestObserver<TransactionDetails>()
        viewModel.loadTransactionDetails(TEST_ID).subscribe(testObserver)
        testObserver.assertFailure(Predicate { error == it })
        then(transactionDetailsRepository).should().loadTransactionDetails(TEST_ID)
        then(transactionDetailsRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadChainHash() {
        val details = "ChainHash"
        given(transactionRepository.loadChainHash(TEST_ID))
            .willReturn(Single.just(details))

        val testObserver = TestObserver<String>()
        viewModel.loadChainHash(TEST_ID).subscribe(testObserver)
        testObserver.assertResult(details)
        then(transactionRepository).should().loadChainHash(TEST_ID)
        then(transactionRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadChainHashError() {
        val error = IllegalStateException()
        given(transactionRepository.loadChainHash(TEST_ID))
            .willReturn(Single.error(error))

        val testObserver = TestObserver<String>()
        viewModel.loadChainHash(TEST_ID).subscribe(testObserver)
        testObserver.assertFailure(Predicate { error == it })
        then(transactionRepository).should().loadChainHash(TEST_ID)
        then(transactionRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeTransactionStatus() {
        val subject = PublishSubject.create<TransactionRepository.PublishStatus>()
        given(transactionRepository.observePublishStatus(TEST_ID)).willReturn(subject)

        val testObserver = TestObserver<TransactionRepository.PublishStatus>()
        viewModel.observeTransactionStatus(TEST_ID).subscribe(testObserver)

        then(transactionRepository).should().observePublishStatus(TEST_ID)
        then(transactionRepository).shouldHaveNoMoreInteractions()
        testObserver.assertEmpty()

        subject.onNext(TransactionRepository.PublishStatus.SUCCESS)
        testObserver.assertValuesOnly(TransactionRepository.PublishStatus.SUCCESS)

        val error = IllegalStateException()
        subject.onError(error)
        testObserver.assertFailure(
            Predicate { error == it },
            TransactionRepository.PublishStatus.SUCCESS
        )

        then(transactionRepository).shouldHaveNoMoreInteractions()
    }

    companion object {
        const val TEST_ID = "some_id"
    }
}
