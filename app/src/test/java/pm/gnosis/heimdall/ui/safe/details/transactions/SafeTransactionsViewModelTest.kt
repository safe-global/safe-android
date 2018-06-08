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
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.db.daos.DescriptionsDao
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository.PublishStatus
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.TransactionStatus
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
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
        val processor = PublishProcessor.create<List<TransactionStatus>>()
        given(safeRepository.observePendingTransactions(testAddress)).willReturn(Flowable.just(emptyList()))
        given(safeRepository.observeSubmittedTransactions(testAddress)).willReturn(processor)
        viewModel.setup(testAddress)

        val subscriber = TestSubscriber<Result<Adapter.Data<SafeTransactionsContract.AdapterEntry>>>()
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

        then(safeRepository).should().observePendingTransactions(testAddress)
        then(safeRepository).should().observeSubmittedTransactions(testAddress)
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

    private fun generateList(
        from: Int = 0,
        to: Int = 0,
        step: Int = 1,
        pending: Boolean = false
    ): List<TransactionStatus> {
        val list = ArrayList<TransactionStatus>(Math.abs(to - from))
        for (i in LongProgression.fromClosedRange(from.toLong(), to.toLong(), step.toLong())) {
            list += TransactionStatus("$i", TEST_TIME, pending)
        }
        return list
    }

    companion object {
        const val TEST_TIME = 123456987L
        val TEST_SAFE = Solidity.Address(BigInteger.TEN)
    }
}
