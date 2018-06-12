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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository.PublishStatus
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.TransactionStatus
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.DateTimeUtils
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger
import java.util.concurrent.TimeoutException

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
    private lateinit var safeTransactionRepository: TransactionExecutionRepository

    @Mock
    private lateinit var detailsRepository: TransactionInfoRepository

    @Mock
    private lateinit var tokenRepository: TokenRepository

    private lateinit var viewModel: SafeTransactionsViewModel

    private var testAddress = Solidity.Address(BigInteger.ZERO)

    @Before
    fun setup() {
        viewModel = SafeTransactionsViewModel(context, safeRepository, safeTransactionRepository, tokenRepository, detailsRepository)
    }

    @Test
    fun observeTransactions() {
        val pendingProcessor = PublishProcessor.create<List<TransactionStatus>>()
        val submittedProcessor = PublishProcessor.create<List<TransactionStatus>>()
        given(safeRepository.observePendingTransactions(testAddress)).willReturn(pendingProcessor)
        given(safeRepository.observeSubmittedTransactions(testAddress)).willReturn(submittedProcessor)
        viewModel.setup(testAddress)

        val subscriber = TestSubscriber<Result<Adapter.Data<SafeTransactionsContract.AdapterEntry>>>()
        viewModel.observeTransactions().subscribe(subscriber)

        // Default AdapterData should be emitted if no cached value is present
        subscriber.assertValueCount(1).assertValueAt(0, {
            it is DataResult && it.data.diff == null && it.data.entries == emptyList<String>()
        }).assertNoErrors()

        var currentId = (subscriber.values().last() as DataResult).data.id

        submittedProcessor.onNext(emptyList())
        pendingProcessor.onNext(
            listOf(
                TransactionStatus("id_1", TEST_TIME, true)
            )
        )

        subscriber.assertValueCount(2).assertValueAt(1, {
            it is DataResult && it.data.parentId == currentId && it.data.diff != null &&
                    it.data.entries == listOf(
                SafeTransactionsContract.AdapterEntry.Header(R.string.header_pending),
                SafeTransactionsContract.AdapterEntry.Transaction("id_1")
            )
        }).assertNoErrors()

        currentId = (subscriber.values().last() as DataResult).data.id

        submittedProcessor.onNext(
            listOf(
                TransactionStatus("id_2", System.currentTimeMillis() - (DateTimeUtils.DAY_IN_MS / 2), false),
                TransactionStatus("id_3", System.currentTimeMillis() - (3 * DateTimeUtils.DAY_IN_MS / 2), false),
                TransactionStatus("id_4", System.currentTimeMillis() - (5 * DateTimeUtils.DAY_IN_MS), false)
            )
        )

        subscriber.assertValueCount(3).assertValueAt(2, {
            it is DataResult && it.data.parentId == currentId &&
                    it.data.entries == listOf(
                SafeTransactionsContract.AdapterEntry.Header(R.string.header_pending),
                SafeTransactionsContract.AdapterEntry.Transaction("id_1"),
                SafeTransactionsContract.AdapterEntry.Header(R.string.header_today),
                SafeTransactionsContract.AdapterEntry.Transaction("id_2"),
                SafeTransactionsContract.AdapterEntry.Header(R.string.header_yesterday),
                SafeTransactionsContract.AdapterEntry.Transaction("id_3"),
                SafeTransactionsContract.AdapterEntry.Header(R.string.header_older),
                SafeTransactionsContract.AdapterEntry.Transaction("id_4")
            )
        }).assertNoErrors()

        currentId = (subscriber.values().last() as DataResult).data.id

        submittedProcessor.onNext(
            listOf(
                TransactionStatus("id_4", System.currentTimeMillis() - (5 * DateTimeUtils.DAY_IN_MS), false)
            )
        )

        subscriber.assertValueCount(4).assertValueAt(3, {
            it is DataResult && it.data.parentId == currentId &&
                    it.data.entries == listOf(
                SafeTransactionsContract.AdapterEntry.Header(R.string.header_pending),
                SafeTransactionsContract.AdapterEntry.Transaction("id_1"),
                SafeTransactionsContract.AdapterEntry.Header(R.string.header_submitted),
                SafeTransactionsContract.AdapterEntry.Transaction("id_4")
            )
        }).assertNoErrors()

        currentId = (subscriber.values().last() as DataResult).data.id

        pendingProcessor.onNext(emptyList())

        subscriber.assertValueCount(5).assertValueAt(4, {
            it is DataResult && it.data.parentId == currentId &&
                    it.data.entries == listOf(
                SafeTransactionsContract.AdapterEntry.Header(R.string.header_submitted),
                SafeTransactionsContract.AdapterEntry.Transaction("id_4")
            )
        }).assertNoErrors()

        then(safeRepository).should().observePendingTransactions(testAddress)
        then(safeRepository).should().observeSubmittedTransactions(testAddress)
        then(safeRepository).shouldHaveNoMoreInteractions()

        subscriber.dispose()

        val cachedSubscriber = TestSubscriber<Result<Adapter.Data<SafeTransactionsContract.AdapterEntry>>>()
        viewModel.observeTransactions().subscribe(cachedSubscriber)

        // On subscribe the last (cached) value should be emitted again
        cachedSubscriber.assertValueCount(1).assertValueAt(0, {
            it is DataResult && it.data.parentId == currentId &&
                    it.data.entries == listOf(
                SafeTransactionsContract.AdapterEntry.Header(R.string.header_submitted),
                SafeTransactionsContract.AdapterEntry.Transaction("id_4")
            )
        }).assertNoErrors()

        currentId = (cachedSubscriber.values().last() as DataResult).data.id

        pendingProcessor.onNext(emptyList())
        submittedProcessor.onNext(listOf(
            TransactionStatus("id_4", System.currentTimeMillis() - (5 * DateTimeUtils.DAY_IN_MS), false),
            TransactionStatus("id_5", System.currentTimeMillis() - (6 * DateTimeUtils.DAY_IN_MS), false),
            TransactionStatus("id_6", System.currentTimeMillis() - (7 * DateTimeUtils.DAY_IN_MS), false)
        ))

        cachedSubscriber.assertValueCount(2).assertValueAt(1, {
            it is DataResult && it.data.parentId == currentId &&
                    it.data.entries == listOf(
                SafeTransactionsContract.AdapterEntry.Header(R.string.header_submitted),
                SafeTransactionsContract.AdapterEntry.Transaction("id_4"),
                SafeTransactionsContract.AdapterEntry.Transaction("id_5"),
                SafeTransactionsContract.AdapterEntry.Transaction("id_6")
            )
        }).assertNoErrors()

        then(safeRepository).should(times(2)).observePendingTransactions(testAddress)
        then(safeRepository).should(times(2)).observeSubmittedTransactions(testAddress)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeTransactionsError() {
        val illegalStateException = IllegalStateException()
        given(safeRepository.observeSubmittedTransactions(testAddress)).willReturn(Flowable.error(illegalStateException))
        given(safeRepository.observePendingTransactions(testAddress)).willReturn(Flowable.error(illegalStateException))
        viewModel.setup(testAddress)

        val subscriber = TestSubscriber<Result<Adapter.Data<SafeTransactionsContract.AdapterEntry>>>()
        viewModel.observeTransactions().subscribe(subscriber)
        subscriber.assertValueCount(2)
            .assertValueAt(0, DataResult(Adapter.Data()))
            .assertValueAt(1, ErrorResult(illegalStateException))
            .assertNoErrors()

        then(safeRepository).should().observePendingTransactions(testAddress)
        then(safeRepository).should().observeSubmittedTransactions(testAddress)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeTransactionsUninitialized() {
        val subscriber = TestSubscriber<Result<Adapter.Data<SafeTransactionsContract.AdapterEntry>>>()
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
    fun loadTransactionInfoDetailsRepoError() {
        val testId = "some_transaction_id"

        val error = IllegalAccessException()
        given(detailsRepository.loadTransactionInfo(anyString()))
            .willReturn(Single.error(error))

        val observer = TestObserver<TransactionInfo>()

        viewModel.setup(testAddress)
        viewModel.loadTransactionInfo(testId)
            .subscribe(observer)

        observer.assertFailure(IllegalAccessException::class.java)

        then(detailsRepository).should().loadTransactionInfo(testId)
        then(detailsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadTransactionInfoNoTransfer() {
        val testId = "some_transaction_id"

        val details = TransactionInfo(
            testId,
            "chain_hash",
            TEST_SAFE,
            TransactionData.Generic(Solidity.Address(BigInteger.TEN), BigInteger.ZERO, null),
            TEST_TIME,
            BigInteger.TEN,
            BigInteger.ZERO,
            ERC20Token.ETHER_TOKEN.address
        )
        given(detailsRepository.loadTransactionInfo(anyString()))
            .willReturn(Single.just(details))

        val observer = TestObserver<TransactionInfo>()

        viewModel.setup(testAddress)
        viewModel.loadTransactionInfo(testId)
            .subscribe(observer)

        observer.assertResult(details)

        then(detailsRepository).should().loadTransactionInfo(testId)
        then(detailsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeTransactionStatus() {
        val testId = "some_transaction_id"
        given(safeTransactionRepository.observePublishStatus(MockUtils.any())).willReturn(Observable.just(PublishStatus.Success(0)))

        val observer = TestObserver<PublishStatus>()
        viewModel.observeTransactionStatus(testId)
            .subscribe(observer)

        observer.assertResult(PublishStatus.Success(0))
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

        observer.assertFailure(Predicate { it is IllegalStateException })
        then(safeTransactionRepository).should().observePublishStatus(testId)
        then(safeTransactionRepository).shouldHaveNoMoreInteractions()
        then(detailsRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadTokenInfo() {
        val testToken = ERC20Token(TEST_TOKEN_ADDRESS, name = "Test Token", symbol = "TT", decimals = 6)
        val expectedSingle = Single.just(testToken)
        given(tokenRepository.loadToken(MockUtils.any())).willReturn(expectedSingle)

        val observer = TestObserver<ERC20Token>()
        val actualSingle = viewModel.loadTokenInfo(TEST_TOKEN_ADDRESS)
        assertEquals(actualSingle, expectedSingle)
        actualSingle.subscribe(observer)

        observer.assertResult(testToken)
        then(tokenRepository).should().loadToken(TEST_TOKEN_ADDRESS)
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadTokenInfoError() {
        val error = NoSuchElementException()
        val expectedSingle = Single.error<ERC20Token>(error)
        given(tokenRepository.loadToken(MockUtils.any())).willReturn(expectedSingle)

        val observer = TestObserver<ERC20Token>()
        val actualSingle = viewModel.loadTokenInfo(TEST_TOKEN_ADDRESS)
        assertEquals(actualSingle, expectedSingle)
        actualSingle.subscribe(observer)

        observer.assertFailure(Predicate { it == error })
        then(tokenRepository).should().loadToken(TEST_TOKEN_ADDRESS)
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    companion object {
        private const val TEST_TIME = 123456987L
        private val TEST_SAFE = Solidity.Address(BigInteger.TEN)
        private val TEST_TOKEN_ADDRESS = "0xa7e15e2e76ab469f8681b576cff168f37aa246ec".asEthereumAddress()!!
    }
}
