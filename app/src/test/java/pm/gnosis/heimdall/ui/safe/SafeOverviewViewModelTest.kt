package pm.gnosis.heimdall.ui.safe

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.observers.TestObserver
import io.reactivex.processors.PublishProcessor
import io.reactivex.subscribers.TestSubscriber
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule
import pm.gnosis.heimdall.test.utils.MockUtils
import pm.gnosis.heimdall.test.utils.TestCompletable
import pm.gnosis.heimdall.test.utils.TestListUpdateCallback
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.safe.overview.SafeOverviewViewModel
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class SafeOverviewViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var repositoryMock: GnosisSafeRepository

    lateinit var viewModel: SafeOverviewViewModel

    @Before
    fun setup() {
        viewModel = SafeOverviewViewModel(repositoryMock)
    }

    @Test
    fun observeSafesResults() {
        val processor = PublishProcessor.create<List<Safe>>()
        val subscriber = createSubscriber()
        given(repositoryMock.observeSafes()).willReturn(processor)

        viewModel.observeSafes().subscribe(subscriber)

        then(repositoryMock).should().observeSafes()
        then(repositoryMock).shouldHaveNoMoreInteractions()
        // Check that the initial value is emitted
        subscriber.assertNoErrors()
                .assertValueCount(1)
                .assertValue { it is DataResult && it.data.parentId == null && it.data.diff == null && it.data.entries.isEmpty() }
        val initialDataId = (subscriber.values().first() as DataResult).data.id

        val results = listOf(Safe(BigInteger.ZERO), Safe(BigInteger.ONE))
        processor.offer(results)
        // Check that the results are emitted
        subscriber.assertNoErrors()
                .assertValueCount(2)
                .assertValueAt(1, { it is DataResult && it.data.parentId == initialDataId && it.data.diff != null && it.data.entries == results })

        val firstData = (subscriber.values()[1] as DataResult).data
        val firstDataId = firstData.id
        val callback = TestListUpdateCallback()
        callback.apply(firstData.diff!!)
                .assertNoChanges().assertNoRemoves().assertNoMoves()
                .assertInsertsCount(1).assertInsert(0)
                .reset()

        val moreResults = listOf(Safe(BigInteger.ONE), Safe(BigInteger.ZERO), Safe(BigInteger.valueOf(3)))
        processor.offer(moreResults)
        // Check that the diff are calculated correctly
        subscriber.assertNoErrors()
                .assertValueCount(3)
                .assertValueAt(2, { it is DataResult && it.data.parentId == firstDataId && it.data.diff != null && it.data.entries == moreResults })

        val secondData = (subscriber.values()[2] as DataResult).data
        callback.apply(secondData.diff!!)
                .assertNoRemoves()
                // A remove might become a move (if it has an insert). So as "1" is "removed" it
                // stays at the end of the list. Once all the removes and inserts are done the moves
                // are calculated. Therefore it is a move from 2 to 0
                .assertMovesCount(1).assertMove(TestListUpdateCallback.Move(2, 0))
                .assertChangesCount(0)
                // Inserts are calculated from the back
                .assertInsertsCount(1).assertInsert(1)
                .reset()
    }

    @Test
    fun observeSafesError() {
        val subscriber = createSubscriber()
        val error = IllegalStateException()
        given(repositoryMock.observeSafes()).willReturn(Flowable.error(error))

        viewModel.observeSafes().subscribe(subscriber)

        then(repositoryMock).should().observeSafes()
        then(repositoryMock).shouldHaveNoMoreInteractions()
        // Check that the results are emitted
        subscriber.assertNoErrors()
                .assertValueCount(2)
                .assertValueAt(0, { it is DataResult && it.data.parentId == null && it.data.diff == null && it.data.entries.isEmpty() })
                .assertValueAt(1, { it is ErrorResult && it.error == error })
    }

    @Test
    fun addSafeSuccess() {
        val observer = TestObserver.create<Unit>()
        val completable = TestCompletable()
        given(repositoryMock.add(MockUtils.any(), anyString())).willReturn(completable)

        viewModel.addSafe(BigInteger.ZERO, "Test").subscribe(observer)

        then(repositoryMock).should().add(BigInteger.ZERO, "Test")
        then(repositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(1, completable.callCount)
        observer.assertTerminated().assertNoErrors().assertNoValues()
    }

    @Test
    fun addSafeError() {
        val observer = TestObserver.create<Unit>()
        val error = IllegalStateException()
        given(repositoryMock.add(MockUtils.any(), anyString())).willReturn(Completable.error(error))

        viewModel.addSafe(BigInteger.ZERO, "Test").subscribe(observer)

        then(repositoryMock).should().add(BigInteger.ZERO, "Test")
        then(repositoryMock).shouldHaveNoMoreInteractions()
        observer.assertTerminated().assertNoValues()
                .assertError(error)
    }

    @Test
    fun removeSafeSuccess() {
        val observer = TestObserver.create<Unit>()
        val completable = TestCompletable()
        given(repositoryMock.remove(MockUtils.any())).willReturn(completable)

        viewModel.removeSafe(BigInteger.ZERO).subscribe(observer)

        then(repositoryMock).should().remove(BigInteger.ZERO)
        then(repositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(1, completable.callCount)
        observer.assertTerminated().assertNoErrors().assertNoValues()
    }

    @Test
    fun removeSafeError() {
        val observer = TestObserver.create<Unit>()
        val error = IllegalStateException()
        given(repositoryMock.remove(MockUtils.any())).willReturn(Completable.error(error))

        viewModel.removeSafe(BigInteger.ZERO).subscribe(observer)

        then(repositoryMock).should().remove(BigInteger.ZERO)
        then(repositoryMock).shouldHaveNoMoreInteractions()
        observer.assertTerminated().assertNoValues()
                .assertError(error)
    }

    @Test
    fun updateSafeNameSuccess() {
        val observer = TestObserver.create<Unit>()
        val completable = TestCompletable()
        given(repositoryMock.updateName(MockUtils.any(), anyString())).willReturn(completable)

        viewModel.updateSafeName(BigInteger.ZERO, "Foo").subscribe(observer)

        then(repositoryMock).should().updateName(BigInteger.ZERO, "Foo")
        then(repositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(1, completable.callCount)
        observer.assertTerminated().assertNoErrors().assertNoValues()
    }

    @Test
    fun updateSafeNameError() {
        val observer = TestObserver.create<Unit>()
        val error = IllegalStateException()
        given(repositoryMock.updateName(MockUtils.any(), anyString())).willReturn(Completable.error(error))

        viewModel.updateSafeName(BigInteger.ZERO, "Foo").subscribe(observer)

        then(repositoryMock).should().updateName(BigInteger.ZERO, "Foo")
        then(repositoryMock).shouldHaveNoMoreInteractions()
        observer.assertTerminated().assertNoValues()
                .assertError(error)
    }

    private fun createSubscriber() = TestSubscriber.create<Result<Adapter.Data<Safe>>>()
}
