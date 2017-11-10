package pm.gnosis.heimdall.ui.safe.details.transactions

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule
import pm.gnosis.heimdall.test.utils.MockUtils
import pm.gnosis.heimdall.test.utils.TestListUpdateCallback
import pm.gnosis.heimdall.test.utils.TestObservableactory
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsContract.PaginatedTransactions
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

    private val testObservableFactory = TestObservableactory<List<String>>()

    @Before
    fun setup() {
        viewModel = SafeTransactionsViewModel(safeRepository)
    }

    @After
    fun tearDown() {
        testObservableFactory.complete()
    }

    @Test
    fun testSetupSameAddress() {
        given(safeRepository.loadDescriptionCount(MockUtils.any())).willReturn(Observable.just(0))

        viewModel.setup(testAddress)
        viewModel.initTransactions(false).subscribe(TestObserver())

        // Setting the same address should keep the cache
        viewModel.setup(testAddress)
        viewModel.initTransactions(false).subscribe(TestObserver())

        then(safeRepository).should(times(1)).loadDescriptionCount(testAddress)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun testSetupDifferentAddress() {
        given(safeRepository.loadDescriptionCount(MockUtils.any())).willReturn(Observable.just(0))

        viewModel.setup(testAddress)
        viewModel.initTransactions(false).subscribe(TestObserver())

        // Setting a different address should clear the cache
        viewModel.setup(BigInteger.TEN)
        viewModel.initTransactions(false).subscribe(TestObserver())

        then(safeRepository).should(times(1)).loadDescriptionCount(testAddress)
        then(safeRepository).should(times(1)).loadDescriptionCount(BigInteger.TEN)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun initTransactionsNoDescriptions() {
        val testObserver = TestObserver<Result<Int>>()
        given(safeRepository.loadDescriptionCount(MockUtils.any())).willReturn(Observable.just(0))

        viewModel.setup(testAddress)
        viewModel.initTransactions(false).subscribe(testObserver)

        testObserver.assertValue(DataResult(0)).assertNoErrors()

        then(safeRepository).should(times(1)).loadDescriptionCount(testAddress)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun initTransactionsError() {
        val error = IllegalStateException()
        val testObserver = TestObserver<Result<Int>>()
        given(safeRepository.loadDescriptionCount(MockUtils.any())).willReturn(Observable.error(error))

        viewModel.setup(testAddress)
        viewModel.initTransactions(false).subscribe(testObserver)

        testObserver.assertValue(ErrorResult(error)).assertNoErrors()

        then(safeRepository).should(times(1)).loadDescriptionCount(testAddress)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun initTransactionsWithDescriptions() {
        given(safeRepository.loadDescriptionCount(MockUtils.any())).willReturn(Observable.just(4))
        given(safeRepository.loadDescriptions(testAddress, 0, 4)).willReturn(Observable.just(generateList(to = 3)))

        viewModel.setup(testAddress)
        val initialObserver = TestObserver<Result<Int>>()
        viewModel.initTransactions(false).subscribe(initialObserver)
        initialObserver.assertValues(DataResult(4)).assertNoErrors()

        // Check that cached version is returned
        val reloadObserver = TestObserver<Result<Int>>()
        viewModel.initTransactions(false).subscribe(reloadObserver)
        reloadObserver.assertValues(DataResult(4)).assertNoErrors()


        then(safeRepository).should(times(1)).loadDescriptionCount(testAddress)
        then(safeRepository).should(times(1)).loadDescriptions(testAddress, 0, 4)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun initTransactionsReload() {
        given(safeRepository.loadDescriptionCount(MockUtils.any())).willReturn(Observable.just(4))
        given(safeRepository.loadDescriptions(testAddress, 0, 4)).willReturn(Observable.just(generateList(to = 3)))

        viewModel.setup(testAddress)
        val initialObserver = TestObserver<Result<Int>>()
        viewModel.initTransactions(false).subscribe(initialObserver)
        initialObserver.assertValues(DataResult(4)).assertNoErrors()

        // Check that cached version is returned
        val reloadObserver = TestObserver<Result<Int>>()
        viewModel.initTransactions(true).subscribe(reloadObserver)
        reloadObserver.assertValues(DataResult(4)).assertNoErrors()


        then(safeRepository).should(times(2)).loadDescriptionCount(testAddress)
        then(safeRepository).should(times(2)).loadDescriptions(testAddress, 0, 4)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeTransactionsReloadFallback() {
        given(safeRepository.loadDescriptionCount(MockUtils.any())).willReturn(Observable.just(8))
        given(safeRepository.loadDescriptions(testAddress, 0, 8)).willReturn(Observable.just(generateList(from = 0, to = 7)))
        viewModel.setup(testAddress)

        val initObserver = TestObserver<Result<Int>>()
        viewModel.initTransactions(false).subscribe(initObserver)
        initObserver.assertValue(DataResult(8)).assertNoErrors()

        // We simulate an error
        val exception = IllegalStateException()
        given(safeRepository.loadDescriptionCount(MockUtils.any())).willReturn(Observable.error(exception))

        val reloadObserver = TestObserver<Result<Int>>()
        viewModel.initTransactions(true).subscribe(reloadObserver)
        reloadObserver.assertValueCount(2)
                .assertValueAt(0, DataResult(8))
                .assertValueAt(1, ErrorResult(exception))
                .assertNoErrors()

        then(safeRepository).should(times(2)).loadDescriptionCount(testAddress)
        then(safeRepository).should(times(1)).loadDescriptions(testAddress, 0, 8)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeTransactionsMore() {
        given(safeRepository.loadDescriptionCount(MockUtils.any())).willReturn(Observable.just(28))
        given(safeRepository.loadDescriptions(testAddress, 8, 28)).willReturn(Observable.just(generateList(from = 8, to = 27)))
        given(safeRepository.loadDescriptions(testAddress, 0, 8)).willReturn(Observable.just(generateList(to = 7)))
        viewModel.setup(testAddress)
        viewModel.initTransactions(false).subscribe(TestObserver())

        val subject = PublishSubject.create<Unit>()
        val moreObserver = TestObserver<Result<PaginatedTransactions>>()
        viewModel.observeTransactions(subject).subscribe(moreObserver)

        moreObserver.assertValueCount(1).assertValueAt(0, {
            it is DataResult && it.data.hasMore &&
                    it.data.data.diff == null && it.data.data.entries == generateList(from = 27, to = 8, step = -1)
        })

        subject.onNext(Unit)

        moreObserver.assertValueCount(2).assertValueAt(1, {
            it is DataResult && !it.data.hasMore &&
                    it.data.data.diff != null && it.data.data.entries == generateList(from = 27, step = -1)
        })

        then(safeRepository).should(times(1)).loadDescriptionCount(testAddress)
        then(safeRepository).should(times(1)).loadDescriptions(testAddress, 8, 28)
        then(safeRepository).should(times(1)).loadDescriptions(testAddress, 0, 8)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeTransactionsMoreLoading() {
        given(safeRepository.loadDescriptionCount(MockUtils.any())).willReturn(Observable.just(28))
        given(safeRepository.loadDescriptions(testAddress, 8, 28)).willReturn(Observable.just(generateList(from = 8, to = 27)))
        given(safeRepository.loadDescriptions(testAddress, 0, 8)).willReturn(testObservableFactory.get())
        viewModel.setup(testAddress)
        viewModel.initTransactions(false).subscribe(TestObserver())

        val subject = PublishSubject.create<Unit>()
        val moreObserver = TestObserver<Result<PaginatedTransactions>>()
        viewModel.observeTransactions(subject).subscribe(moreObserver)

        moreObserver.assertValueCount(1).assertValueAt(0, {
            it is DataResult && it.data.hasMore &&
                    it.data.data.diff == null && it.data.data.entries == generateList(from = 27, to = 8, step = -1)
        })

        subject.onNext(Unit)

        moreObserver.assertValueCount(1)

        // Triggering load more again should not do anything while it is loading
        subject.onNext(Unit)

        testObservableFactory.success(generateList(to = 7))

        moreObserver.assertValueCount(2).assertValueAt(1, {
            it is DataResult && !it.data.hasMore &&
                    it.data.data.diff != null && it.data.data.entries == generateList(from = 27, step = -1)
        })

        TestListUpdateCallback().apply((moreObserver.values()[1] as DataResult).data.data.diff!!)
                .assertNoMoves()
                .assertNoRemoves()
                .assertInsertsCount(8).assertInserts(20, 8)

        then(safeRepository).should(times(1)).loadDescriptionCount(testAddress)
        then(safeRepository).should(times(1)).loadDescriptions(testAddress, 8, 28)
        then(safeRepository).should(times(1)).loadDescriptions(testAddress, 0, 8)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeTransactionsError() {
        val error = IllegalStateException()
        given(safeRepository.loadDescriptionCount(MockUtils.any())).willReturn(Observable.just(28))
        given(safeRepository.loadDescriptions(testAddress, 8, 28)).willReturn(Observable.just(generateList(from = 8, to = 27)))
        given(safeRepository.loadDescriptions(testAddress, 0, 8)).willReturn(Observable.error(error))
        viewModel.setup(testAddress)
        viewModel.initTransactions(false).subscribe(TestObserver())

        val subject = PublishSubject.create<Unit>()
        val moreObserver = TestObserver<Result<PaginatedTransactions>>()
        viewModel.observeTransactions(subject).subscribe(moreObserver)

        moreObserver.assertValueCount(1).assertValueAt(0, {
            it is DataResult && it.data.hasMore &&
                    it.data.data.diff == null && it.data.data.entries == generateList(from = 27, to = 8, step = -1)
        })

        subject.onNext(Unit)

        moreObserver.assertValueCount(2).assertValueAt(1, {
            it is ErrorResult && it.error == error
        })

        then(safeRepository).should(times(1)).loadDescriptionCount(testAddress)
        then(safeRepository).should(times(1)).loadDescriptions(testAddress, 8, 28)
        then(safeRepository).should(times(1)).loadDescriptions(testAddress, 0, 8)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeTransactionsUninitialized() {
        viewModel.setup(testAddress)

        val subject = PublishSubject.create<Unit>()
        val moreObserver = TestObserver<Result<PaginatedTransactions>>()
        viewModel.observeTransactions(subject).subscribe(moreObserver)

        moreObserver.assertValueCount(1).assertValueAt(0, {
            it is DataResult && !it.data.hasMore &&
                    it.data.data.diff == null && it.data.data.entries.isEmpty()
        })

        subject.onNext(Unit)

        moreObserver.assertValueCount(2).assertValueAt(1, {
            it is DataResult && !it.data.hasMore &&
                    it.data.data.diff != null && it.data.data.entries.isEmpty()
        })

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