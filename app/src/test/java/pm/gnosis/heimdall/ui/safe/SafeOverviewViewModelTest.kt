package pm.gnosis.heimdall.ui.safe

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.ui.safe.overview.SafeOverviewViewModel
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class SafeOverviewViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var repositoryMock: GnosisSafeRepository

    private lateinit var viewModel: SafeOverviewViewModel

    @Before
    fun setup() {
        viewModel = SafeOverviewViewModel(repositoryMock)
    }

    @Test
    fun loadSafeInfoOnErrorLoadsFromCache() {
        val testObserver = TestObserver.create<SafeInfo>()
        val subject = PublishSubject.create<SafeInfo>()
        val safeInfo = SafeInfo("0x0", Wei(BigInteger.ZERO), 0, emptyList(), false, emptyList())
        given(repositoryMock.loadInfo(MockUtils.any())).willReturn(subject)

        viewModel.loadSafeInfo(Solidity.Address(BigInteger.ZERO)).subscribe(testObserver)
        subject.onNext(safeInfo)

        testObserver.assertValue(safeInfo)

        // Error loading the same safe (eg.: no internet) -> should load from cache
        val testObserver2 = TestObserver.create<SafeInfo>()
        viewModel.loadSafeInfo(Solidity.Address(BigInteger.ZERO)).subscribe(testObserver2)
        subject.onError(Exception())

        testObserver2.assertResult(safeInfo)
        then(repositoryMock).should(times(2)).loadInfo(Solidity.Address(BigInteger.ZERO))
        then(repositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadSafeInfoError() {
        val testObserver = TestObserver.create<SafeInfo>()
        val exception = Exception()
        given(repositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.error(exception))

        viewModel.loadSafeInfo(Solidity.Address(BigInteger.ZERO)).subscribe(testObserver)

        then(repositoryMock).should().loadInfo(Solidity.Address(BigInteger.ZERO))
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
}
