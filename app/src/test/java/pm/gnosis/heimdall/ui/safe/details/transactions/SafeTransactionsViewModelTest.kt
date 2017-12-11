package pm.gnosis.heimdall.ui.safe.details.transactions

import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class SafeTransactionsViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var safeRepository: GnosisSafeRepository

    private lateinit var viewModel: SafeTransactionsViewModel

    private var testAddress = BigInteger.ZERO

    @Before
    fun setup() {
        viewModel = SafeTransactionsViewModel(safeRepository)
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

    private fun generateList(from: Int = 0, to: Int = 0, step: Int = 1): List<String> {
        val list = ArrayList<String>(Math.abs(to - from))
        for (i in LongProgression.fromClosedRange(from.toLong(), to.toLong(), step.toLong() )) {
            list += "$i"
        }
        return list
    }

}