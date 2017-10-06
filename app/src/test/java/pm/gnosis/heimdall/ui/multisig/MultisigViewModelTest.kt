package pm.gnosis.heimdall.ui.multisig

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.observers.TestObserver
import io.reactivex.processors.PublishProcessor
import io.reactivex.subscribers.TestSubscriber
import junit.framework.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.common.util.DataResult
import pm.gnosis.heimdall.common.util.ErrorResult
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.data.repositories.MultisigRepository
import pm.gnosis.heimdall.data.repositories.model.MultisigWallet
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule
import pm.gnosis.heimdall.test.utils.MockUtils.eq
import pm.gnosis.heimdall.test.utils.TestCompletable
import pm.gnosis.heimdall.test.utils.TestListUpdateCallback
import pm.gnosis.heimdall.ui.base.Adapter
import org.mockito.Mockito.`when` as given

@RunWith(MockitoJUnitRunner::class)
class MultisigViewModelTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var repository: MultisigRepository

    lateinit var viewModel: MultisigContract

    @Before
    fun setup() {
        viewModel = MultisigViewModel(repository)
    }

    @Test
    fun observeMultisigWalletsResults() {
        val processor = PublishProcessor.create<List<MultisigWallet>>()
        val subscriber = createSubscriber()
        given(repository.observeMultisigWallets()).thenReturn(processor)
        viewModel.observeMultisigWallets().subscribe(subscriber)

        // Check that the initial value is emitted
        subscriber.assertNoErrors()
                .assertValueCount(1)
                .assertValue { it is DataResult && it.data.parentId == null && it.data.diff == null && it.data.entries.isEmpty() }
        val initialDataId = (subscriber.values().first() as DataResult).data.id

        val results = listOf(MultisigWallet("0"), MultisigWallet("1"))
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

        val moreResults = listOf(MultisigWallet("1"), MultisigWallet("0"), MultisigWallet("3"))
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
                .assertChangesCount(1).assertChange(0)
                // Inserts are calculated from the back
                .assertInsertsCount(1).assertInsert(1)
                .reset()
    }

    @Test
    fun observeMultisigWalletsError() {
        val subscriber = createSubscriber()
        val error = IllegalStateException()
        given(repository.observeMultisigWallets()).thenReturn(Flowable.error(error))
        viewModel.observeMultisigWallets().subscribe(subscriber)

        // Check that the results are emitted
        subscriber.assertNoErrors()
                .assertValueCount(2)
                .assertValueAt(0, { it is DataResult && it.data.parentId == null && it.data.diff == null && it.data.entries.isEmpty() })
                .assertValueAt(1, { it is ErrorResult && it.error == error })
    }

    @Test
    fun addMultisigWalletSuccess() {
        val observer = TestObserver.create<Unit>()
        val completable = TestCompletable()
        given(repository.addMultisigWallet(anyString(), anyString())).thenReturn(completable)

        viewModel.addMultisigWallet("Test", "Test").subscribe(observer)
        Assert.assertEquals(1, completable.callCount)
        observer.assertTerminated().assertNoErrors().assertNoValues()
    }

    @Test
    fun addMultisigWalletError() {
        val observer = TestObserver.create<Unit>()
        val error = IllegalStateException()
        given(repository.addMultisigWallet(eq("Test"), anyString())).thenReturn(Completable.error(error))

        viewModel.addMultisigWallet("Test", "Test").subscribe(observer)
        observer.assertTerminated().assertNoValues()
                .assertError(error)
    }

    @Test
    fun removeMultisigWalletSuccess() {
        val observer = TestObserver.create<Unit>()
        val completable = TestCompletable()
        given(repository.removeMultisigWallet(eq("Test"))).thenReturn(completable)

        viewModel.removeMultisigWallet("Test").subscribe(observer)
        Assert.assertEquals(1, completable.callCount)
        observer.assertTerminated().assertNoErrors().assertNoValues()
    }

    @Test
    fun removeMultisigWalletError() {
        val observer = TestObserver.create<Unit>()
        val error = IllegalStateException()
        given(repository.removeMultisigWallet(eq("Test"))).thenReturn(Completable.error(error))

        viewModel.removeMultisigWallet("Test").subscribe(observer)
        observer.assertTerminated().assertNoValues()
                .assertError(error)
    }

    @Test
    fun updateMultisigWalletNameSuccess() {
        val observer = TestObserver.create<Unit>()
        val completable = TestCompletable()
        given(repository.updateMultisigWalletName(eq("Test"), anyString())).thenReturn(completable)

        viewModel.updateMultisigWalletName("Test", "Foo").subscribe(observer)
        Assert.assertEquals(1, completable.callCount)
        observer.assertTerminated().assertNoErrors().assertNoValues()
    }

    @Test
    fun updateMultisigWalletNameError() {
        val observer = TestObserver.create<Unit>()
        val error = IllegalStateException()
        given(repository.updateMultisigWalletName(eq("Test"), anyString())).thenReturn(Completable.error(error))

        viewModel.updateMultisigWalletName("Test", "Foo").subscribe(observer)
        observer.assertTerminated().assertNoValues()
                .assertError(error)
    }

    private fun createSubscriber() =
            TestSubscriber.create<Result<Adapter.Data<MultisigWallet>>>()

}