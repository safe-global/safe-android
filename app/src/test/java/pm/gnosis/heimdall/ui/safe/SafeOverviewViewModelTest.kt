package pm.gnosis.heimdall.ui.safe

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.processors.PublishProcessor
import io.reactivex.subjects.PublishSubject
import io.reactivex.subscribers.TestSubscriber
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.safe.overview.SafeOverviewViewModel
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.*
import java.math.BigInteger
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
class SafeOverviewViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var accountsRepositoryMock: AccountsRepository

    @Mock
    private lateinit var ethereumRepositoryMock: EthereumRepository

    @Mock
    private lateinit var repositoryMock: GnosisSafeRepository

    private lateinit var viewModel: SafeOverviewViewModel

    private val testPreferences = TestPreferences()

    @Before
    fun setup() {
        given(contextMock.getSharedPreferences(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())).willReturn(testPreferences)
        viewModel = SafeOverviewViewModel(
            accountsRepositoryMock, ethereumRepositoryMock,
            PreferencesManager(contextMock), repositoryMock
        )
    }

    @Test
    fun observeSafesResults() {
        val processor = PublishProcessor.create<List<AbstractSafe>>()
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
            .assertInsertsCount(2).assertInserts(0, 2)
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
    fun removeSafeSuccess() {
        val observer = TestObserver.create<Unit>()
        val completable = TestCompletable()
        given(repositoryMock.removeSafe(MockUtils.any())).willReturn(completable)

        viewModel.removeSafe(BigInteger.ZERO).subscribe(observer)

        then(repositoryMock).should().removeSafe(BigInteger.ZERO)
        then(repositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(1, completable.callCount)
        observer.assertTerminated().assertNoErrors().assertNoValues()
    }

    @Test
    fun removeSafeError() {
        val observer = TestObserver.create<Unit>()
        val error = IllegalStateException()
        given(repositoryMock.removeSafe(MockUtils.any())).willReturn(Completable.error(error))

        viewModel.removeSafe(BigInteger.ZERO).subscribe(observer)

        then(repositoryMock).should().removeSafe(BigInteger.ZERO)
        then(repositoryMock).shouldHaveNoMoreInteractions()
        observer.assertTerminated().assertNoValues()
            .assertError(error)
    }

    @Test
    fun loadSafeInfoOnErrorLoadsFromCache() {
        val testObserver = TestObserver.create<SafeInfo>()
        val subject = PublishSubject.create<SafeInfo>()
        val safeInfo = SafeInfo("0x0", Wei(BigInteger.ZERO), 0, emptyList(), false)
        given(repositoryMock.loadInfo(MockUtils.any())).willReturn(subject)

        viewModel.loadSafeInfo(BigInteger.ZERO).subscribe(testObserver)
        subject.onNext(safeInfo)

        testObserver.assertValue(safeInfo)

        // Error loading the same safe (eg.: no internet) -> should load from cache
        val testObserver2 = TestObserver.create<SafeInfo>()
        viewModel.loadSafeInfo(BigInteger.ZERO).subscribe(testObserver2)
        subject.onError(Exception())

        testObserver2.assertResult(safeInfo)
        then(repositoryMock).should(times(2)).loadInfo(BigInteger.ZERO)
        then(repositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadSafeInfoError() {
        val testObserver = TestObserver.create<SafeInfo>()
        val exception = Exception()
        given(repositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.error(exception))

        viewModel.loadSafeInfo(BigInteger.ZERO).subscribe(testObserver)

        then(repositoryMock).should().loadInfo(BigInteger.ZERO)
        then(repositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertError(exception)
    }

    @Test
    fun observeDeployedStatus() {
        val result = "result"
        val testObserver = TestObserver.create<String>()
        given(repositoryMock.observeDeployStatus(anyString())).willReturn(Observable.just(result))

        viewModel.observeDeployStatus("test").subscribe(testObserver)

        then(repositoryMock).should().observeDeployStatus("test")
        then(repositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(result)
    }

    @Test
    fun observeDeployedStatusError() {
        val exception = Exception()
        val testObserver = TestObserver.create<String>()
        given(repositoryMock.observeDeployStatus(anyString())).willReturn(Observable.error(exception))

        viewModel.observeDeployStatus("test").subscribe(testObserver)

        then(repositoryMock).should().observeDeployStatus("test")
        then(repositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertError(exception)
    }

    @Test
    fun shouldShowLowBalanceViewOnHighBalance() = onTestableComputationScheduler {
        val testObserver = TestObserver<Result<Boolean>>()
        val account = Account(BigInteger.ZERO)
        val balance = LOW_BALANCE_THRESHOLD

        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))

        // First emission
        given(ethereumRepositoryMock.getBalance(MockUtils.any())).willReturn(Observable.just(balance))

        viewModel.shouldShowLowBalanceView().subscribe(testObserver)

        assertFalse(testPreferences.getBoolean(DISMISS_LOW_BALANCE, true))
        testObserver
            .assertValueAt(0, DataResult(false))
            .assertNotComplete()

        // Second emission
        given(ethereumRepositoryMock.getBalance(MockUtils.any())).willReturn(Observable.just(balance))

        viewModel.shouldShowLowBalanceView().subscribe(testObserver)

        it.advanceTimeBy(BALANCE_CHECK_TIME_INTERVAL_SECONDS, TimeUnit.SECONDS)
        assertFalse(testPreferences.getBoolean(DISMISS_LOW_BALANCE, true))
        testObserver
            .assertValueAt(1, DataResult(false))
            .assertNotComplete()


        // Third emission
        val exception = Exception()
        given(ethereumRepositoryMock.getBalance(MockUtils.any())).willReturn(Observable.error(exception))

        viewModel.shouldShowLowBalanceView().subscribe(testObserver)

        it.advanceTimeBy(BALANCE_CHECK_TIME_INTERVAL_SECONDS, TimeUnit.SECONDS)
        assertFalse(testPreferences.getBoolean(DISMISS_LOW_BALANCE, true))
        testObserver
            .assertValueAt(2, ErrorResult(exception))
            .assertNotComplete()

        then(accountsRepositoryMock).should(times(3)).loadActiveAccount()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(ethereumRepositoryMock).should(times(3)).getBalance(account.address)
        then(ethereumRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun shouldShowLowBalanceViewOnLowBalance() = onTestableComputationScheduler {
        val testObserver = TestObserver<Result<Boolean>>()
        val account = Account(BigInteger.ZERO)
        val balance = Wei(LOW_BALANCE_THRESHOLD.value - BigInteger.ONE)
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))

        // First emission (balance)
        given(ethereumRepositoryMock.getBalance(MockUtils.any())).willReturn(Observable.just(balance))

        viewModel.shouldShowLowBalanceView().subscribe(testObserver)

        testObserver
            .assertValueAt(0, DataResult(true))
            .assertNotComplete()

        // Second emission (balance)
        given(ethereumRepositoryMock.getBalance(MockUtils.any())).willReturn(Observable.just(balance))

        viewModel.shouldShowLowBalanceView().subscribe(testObserver)

        it.advanceTimeBy(BALANCE_CHECK_TIME_INTERVAL_SECONDS, TimeUnit.SECONDS)
        testObserver
            .assertValueAt(1, DataResult(true))
            .assertNotComplete()

        // Third emission (error)
        val exception = Exception()
        given(ethereumRepositoryMock.getBalance(MockUtils.any())).willReturn(Observable.error(exception))

        viewModel.shouldShowLowBalanceView().subscribe(testObserver)
        it.advanceTimeBy(BALANCE_CHECK_TIME_INTERVAL_SECONDS, TimeUnit.SECONDS)

        testObserver
            .assertValueAt(2, ErrorResult(exception))
            .assertNotComplete()

        then(accountsRepositoryMock).should(times(3)).loadActiveAccount()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(ethereumRepositoryMock).should(times(3)).getBalance(account.address)
        then(ethereumRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun onceDismissedDoNotShowOnLowBalance() = onTestableComputationScheduler {
        val testObserver = TestObserver<Result<Boolean>>()
        val account = Account(BigInteger.ZERO)
        val balance = Wei(LOW_BALANCE_THRESHOLD.value - BigInteger.ONE)

        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(ethereumRepositoryMock.getBalance(MockUtils.any())).willReturn(Observable.just(balance))

        viewModel.dismissHasLowBalance()
        assertTrue(testPreferences.getBoolean(DISMISS_LOW_BALANCE, false))
        viewModel.shouldShowLowBalanceView().subscribe(testObserver)

        // First emission (balance)
        testObserver
            .assertValueAt(0, DataResult(false))
            .assertNotComplete()

        // Second emission (error)
        // even if we have an error it should not show
        given(ethereumRepositoryMock.getBalance(MockUtils.any())).willReturn(Observable.just(balance))

        viewModel.shouldShowLowBalanceView().subscribe(testObserver)

        it.advanceTimeBy(BALANCE_CHECK_TIME_INTERVAL_SECONDS, TimeUnit.SECONDS)
        testObserver
            .assertValueAt(1, DataResult(false))
            .assertNotComplete()

        then(accountsRepositoryMock).should(times(2)).loadActiveAccount()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(ethereumRepositoryMock).should(times(2)).getBalance(account.address)
        then(ethereumRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun lowBalanceToHighBalanceShouldTurnDismissOff() = onTestableComputationScheduler {
        val testObserver = TestObserver<Result<Boolean>>()
        val account = Account(BigInteger.ZERO)
        val balance = Wei(LOW_BALANCE_THRESHOLD.value)
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(ethereumRepositoryMock.getBalance(MockUtils.any())).willReturn(Observable.just(balance))

        viewModel.dismissHasLowBalance()
        assertTrue(testPreferences.getBoolean(DISMISS_LOW_BALANCE, false))
        viewModel.shouldShowLowBalanceView().subscribe(testObserver)

        // First emission (low balance)
        given(ethereumRepositoryMock.getBalance(MockUtils.any())).willReturn(Observable.just(balance))
        testObserver
            .assertValueAt(0, DataResult(false))
            .assertNotComplete()

        // Second emission (higher balance)
        given(ethereumRepositoryMock.getBalance(MockUtils.any())).willReturn(Observable.just(balance))
        it.advanceTimeBy(BALANCE_CHECK_TIME_INTERVAL_SECONDS, TimeUnit.SECONDS)
        testObserver
            .assertValueAt(1, DataResult(false))
            .assertNotComplete()
        assertFalse(testPreferences.getBoolean(DISMISS_LOW_BALANCE, true))

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(ethereumRepositoryMock).should(times(2)).getBalance(account.address)
        then(ethereumRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun dismissHasLowBalance() {
        viewModel.dismissHasLowBalance()

        assertTrue(testPreferences.getBoolean(DISMISS_LOW_BALANCE, false))
    }

    private fun createSubscriber() = TestSubscriber.create<Result<Adapter.Data<AbstractSafe>>>()

    companion object {
        private val LOW_BALANCE_THRESHOLD = Wei.ether("0.001")
        private const val DISMISS_LOW_BALANCE = "prefs.boolean.dismiss_low_balance"
        private const val BALANCE_CHECK_TIME_INTERVAL_SECONDS = 10L
    }
}
