package pm.gnosis.heimdall.ui.safe.details.transactions

import android.content.Context
import android.content.Intent
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Predicate
import io.reactivex.observers.TestObserver
import io.reactivex.processors.PublishProcessor
import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.TransactionRepository.PublishStatus
import pm.gnosis.heimdall.data.repositories.impls.GnosisSafeTransactionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.mockGetString
import java.math.BigInteger
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class SafeTransactionsViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var safeRepository: GnosisSafeRepository

    @Mock
    private lateinit var safeTransactionRepository: GnosisSafeTransactionRepository

    @Mock
    private lateinit var detailsRepository: TransactionDetailsRepository

    @Mock
    private lateinit var tokenRepository: TokenRepository

    private lateinit var viewModel: SafeTransactionsViewModel

    private var testAddress = BigInteger.ZERO

    @Before
    fun setup() {
        context.mockGetString()
        viewModel = SafeTransactionsViewModel(context, safeRepository, safeTransactionRepository, tokenRepository, detailsRepository)
    }

    @Test
    fun observeTransactions() {
        val processor = PublishProcessor.create<List<String>>()
        given(safeRepository.observeTransactionDescriptions(testAddress)).willReturn(processor)
        viewModel.setup(testAddress)

        val subscriber = TestSubscriber<Result<Adapter.Data<String>>>()
        viewModel.observeTransactions().subscribe(subscriber)

        subscriber.assertValueCount(1).assertValueAt(0, {
            it is DataResult &&
                    it.data.diff == null && it.data.entries == emptyList<String>()
        }).assertNoErrors()

        val initialDataId = (subscriber.values().first() as DataResult).data.id

        processor.onNext(generateList(to = 8))

        subscriber.assertValueCount(2).assertValueAt(1, {
            it is DataResult && it.data.parentId == initialDataId &&
                    it.data.diff != null && it.data.entries == generateList(to = 8)
        }).assertNoErrors()

        val firstDataId = (subscriber.values()[1] as DataResult).data.id

        processor.onNext(generateList(from = 1, to = 9))

        subscriber.assertValueCount(3).assertValueAt(2, {
            it is DataResult && it.data.parentId == firstDataId &&
                    it.data.diff != null && it.data.entries == generateList(from = 1, to = 9)
        }).assertNoErrors()

        then(safeRepository).should().observeTransactionDescriptions(testAddress)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeTransactionsError() {
        val illegalStateException = IllegalStateException()
        given(safeRepository.observeTransactionDescriptions(testAddress)).willReturn(Flowable.error(illegalStateException))
        viewModel.setup(testAddress)

        val subscriber = TestSubscriber<Result<Adapter.Data<String>>>()
        viewModel.observeTransactions().subscribe(subscriber)
        subscriber.assertValueCount(2)
                .assertValueAt(0, DataResult(Adapter.Data()))
                .assertValueAt(1, ErrorResult(illegalStateException))
                .assertNoErrors()

        then(safeRepository).should().observeTransactionDescriptions(testAddress)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeTransactionsUninitialized() {
        val subscriber = TestSubscriber<Result<Adapter.Data<String>>>()
        viewModel.observeTransactions().subscribe(subscriber)

        subscriber.assertNoErrors().assertNoValues().assertComplete()

        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun transactionSelected() {
        val observer = TestObserver<Intent>()
        // Should never return an error even if transaction is invalid
        viewModel.transactionSelected("")
                .subscribe(observer)

        observer.assertNoErrors().assertComplete().assertValueCount(1)
    }

    @Test
    fun loadTransactionDetailsNoSafe() {
        val observer = TestObserver<Pair<TransactionDetails, SafeTransactionsContract.TransferInfo?>>()
        viewModel.loadTransactionDetails("TEST ID")
                .subscribe(observer)

        observer.assertFailure(IllegalStateException::class.java)

        then(detailsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadTransactionDetailsDetailsRepoError() {
        val testId = "some_transaction_id"

        val error = IllegalAccessException()
        given(detailsRepository.loadTransactionDetails(anyString()))
                .willReturn(Single.error(error))

        val observer = TestObserver<Pair<TransactionDetails, SafeTransactionsContract.TransferInfo?>>()

        viewModel.setup(testAddress)
        viewModel.loadTransactionDetails(testId)
                .subscribe(observer)

        observer.assertFailure(IllegalAccessException::class.java)

        then(detailsRepository).should().loadTransactionDetails(testId)
        then(detailsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadTransactionDetailsNoTransfer() {
        val testId = "some_transaction_id"

        val details = TransactionDetails(testId, TransactionType.GENERIC, null, Transaction(BigInteger.TEN), TEST_SAFE, TEST_TIME)
        given(detailsRepository.loadTransactionDetails(anyString()))
                .willReturn(Single.just(details))

        val observer = TestObserver<Pair<TransactionDetails, SafeTransactionsContract.TransferInfo?>>()

        viewModel.setup(testAddress)
        viewModel.loadTransactionDetails(testId)
                .subscribe(observer)

        observer.assertResult(details to null)

        then(detailsRepository).should().loadTransactionDetails(testId)
        then(detailsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadTransactionDetailsNoTokenTransferData() {
        val testId = "some_transaction_id"

        val details = TransactionDetails(testId, TransactionType.TOKEN_TRANSFER, null, Transaction(BigInteger.TEN), TEST_SAFE, TEST_TIME)
        given(detailsRepository.loadTransactionDetails(anyString()))
                .willReturn(Single.just(details))

        val observer = TestObserver<Pair<TransactionDetails, SafeTransactionsContract.TransferInfo?>>()

        viewModel.setup(testAddress)
        viewModel.loadTransactionDetails(testId)
                .subscribe(observer)

        observer.assertResult(details to null)

        then(detailsRepository).should().loadTransactionDetails(testId)
        then(detailsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadTransactionDetailsTokenTransferUnknownToken() {
        val testId = "some_transaction_id"
        val tokenAddress = BigInteger.TEN
        val tokenTransferData = TokenTransferData(BigInteger.ONE, BigInteger.valueOf(42))

        val details = TransactionDetails(testId, TransactionType.TOKEN_TRANSFER, tokenTransferData, Transaction(tokenAddress), TEST_SAFE, TEST_TIME)
        given(detailsRepository.loadTransactionDetails(anyString()))
                .willReturn(Single.just(details))
        given(tokenRepository.observeToken(MockUtils.any())).willReturn(Flowable.empty())

        val observer = TestObserver<Pair<TransactionDetails, SafeTransactionsContract.TransferInfo?>>()

        viewModel.setup(testAddress)
        viewModel.loadTransactionDetails(testId)
                .subscribe(observer)

        observer.assertResult(details to null)

        then(detailsRepository).should().loadTransactionDetails(testId)
        then(detailsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepository).should().observeToken(tokenAddress)
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadTransactionDetailsTokenTransfer() {
        val testId = "some_transaction_id"
        val tokenAddress = BigInteger.TEN
        val tokenTransferData = TokenTransferData(BigInteger.ONE, BigInteger.valueOf(42))

        val details = TransactionDetails(testId, TransactionType.TOKEN_TRANSFER, tokenTransferData, Transaction(tokenAddress), TEST_SAFE, TEST_TIME)
        given(detailsRepository.loadTransactionDetails(anyString()))
                .willReturn(Single.just(details))
        given(tokenRepository.observeToken(MockUtils.any())).willReturn(Flowable.just(ERC20Token(tokenAddress, decimals = 0, symbol = "GNO")))

        val observer = TestObserver<Pair<TransactionDetails, SafeTransactionsContract.TransferInfo?>>()

        viewModel.setup(testAddress)
        viewModel.loadTransactionDetails(testId)
                .subscribe(observer)

        observer.assertResult(details to SafeTransactionsContract.TransferInfo("42", "GNO"))

        then(detailsRepository).should().loadTransactionDetails(testId)
        then(detailsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepository).should().observeToken(tokenAddress)
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadTransactionDetailsEtherTransfer() {
        val testId = "some_transaction_id"

        val amount = BigInteger.valueOf(42) * BigInteger.TEN.pow(18)
        val details = TransactionDetails(testId, TransactionType.ETHER_TRANSFER, null, Transaction(BigInteger.ONE, value = Wei(amount)), TEST_SAFE, TEST_TIME)
        given(detailsRepository.loadTransactionDetails(anyString()))
                .willReturn(Single.just(details))

        val observer = TestObserver<Pair<TransactionDetails, SafeTransactionsContract.TransferInfo?>>()

        viewModel.setup(testAddress)
        viewModel.loadTransactionDetails(testId)
                .subscribe(observer)

        observer.assertResult(details to SafeTransactionsContract.TransferInfo("42", R.string.currency_eth.toString()))

        then(detailsRepository).should().loadTransactionDetails(testId)
        then(detailsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeTransactionStatus() {
        val testId = "some_transaction_id"
        given(safeTransactionRepository.observePublishStatus(MockUtils.any())).willReturn(Observable.just(PublishStatus.SUCCESS))

        val observer = TestObserver<PublishStatus>()
        viewModel.observeTransactionStatus(testId)
                .subscribe(observer)

        observer.assertResult(PublishStatus.SUCCESS)
        then(safeTransactionRepository).should().observePublishStatus(testId)
        then(safeTransactionRepository).shouldHaveNoMoreInteractions()
        then(detailsRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeTransactionStatusError() {
        val testId = "some_transaction_id"
        given(safeTransactionRepository.observePublishStatus(MockUtils.any())).willReturn(Observable.error(IllegalStateException()))

        val observer = TestObserver<PublishStatus>()
        viewModel.observeTransactionStatus(testId)
                .subscribe(observer)

        observer.assertFailure(Predicate{ it is IllegalStateException })
        then(safeTransactionRepository).should().observePublishStatus(testId)
        then(safeTransactionRepository).shouldHaveNoMoreInteractions()
        then(detailsRepository).shouldHaveNoMoreInteractions()
    }

    private fun generateList(from: Int = 0, to: Int = 0, step: Int = 1): List<String> {
        val list = ArrayList<String>(Math.abs(to - from))
        for (i in LongProgression.fromClosedRange(from.toLong(), to.toLong(), step.toLong())) {
            list += "$i"
        }
        return list
    }

    companion object {
        const val TEST_TIME = 123456987L
        val TEST_SAFE = BigInteger.TEN
    }

}