package pm.gnosis.heimdall.ui.safe.details.info

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.processors.PublishProcessor
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestCompletable
import pm.gnosis.tests.utils.mockGetString
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class SafeSettingsViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var repositoryMock: GnosisSafeRepository

    lateinit var viewModel: SafeSettingsViewModel

    @Before
    fun setup() {
        contextMock.mockGetString()
        viewModel = SafeSettingsViewModel(contextMock, repositoryMock)
    }

    private fun callSetupAndCheck(address: BigInteger, info: SafeInfo,
                                  repositoryInvocations: Int = 1, totalInvocations: Int = repositoryInvocations,
                                  ignoreCached: Boolean = false) {
        // Setup with address
        viewModel.setup(address)

        // Verify that the repositoryMock is called and expected version is returned
        val observer = TestObserver.create<Result<SafeInfo>>()
        viewModel.loadSafeInfo(ignoreCached).subscribe(observer)
        observer.assertNoErrors().assertValueCount(1).assertValue(DataResult(info))
        then(repositoryMock).should(times(repositoryInvocations)).loadInfo(address)
        then(repositoryMock).should(times(totalInvocations)).loadInfo(MockUtils.any())
    }

    @Test
    fun setupViewModelClearCache() {
        val address1 = BigInteger.ZERO
        val info1 = SafeInfo("Test1", Wei(BigInteger.ONE), 0, emptyList(), false)
        given(repositoryMock.loadInfo(address1)).willReturn(Observable.just(info1))

        val address2 = BigInteger.ONE
        val info2 = SafeInfo("Test2", Wei(BigInteger.ONE), 0, emptyList(), false)
        given(repositoryMock.loadInfo(address2)).willReturn(Observable.just(info2))

        callSetupAndCheck(address1, info1)

        callSetupAndCheck(address2, info2, totalInvocations = 2)
    }

    @Test
    fun setupViewModelKeepCache() {
        val address = BigInteger.ZERO
        val info = SafeInfo("Test", Wei(BigInteger.ONE), 0, emptyList(), false)
        given(repositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.just(info))

        callSetupAndCheck(address, info)

        callSetupAndCheck(address, info)
    }

    @Test
    fun loadSafeInfoIgnoreCache() {
        val address = BigInteger.ZERO
        val info = SafeInfo("Test", Wei(BigInteger.ONE), 0, emptyList(), false)
        given(repositoryMock.loadInfo(address)).willReturn(Observable.just(info))

        callSetupAndCheck(address, info)

        callSetupAndCheck(address, info, 2, ignoreCached = true)
    }

    @Test
    fun loadSafeInfoError() {
        viewModel.setup(BigInteger.ZERO)

        val exception = IllegalStateException("test")
        given(repositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.error(exception))

        val observer = TestObserver.create<Result<SafeInfo>>()
        viewModel.loadSafeInfo(true).subscribe(observer)
        observer.assertNoErrors().assertValueCount(1).assertValue(ErrorResult(exception))
    }

    @Test
    fun changeSafeName() {
        val newName = "newName     with    too many spaces   "
        val cleanName = "newName with too many spaces"
        val testCompletable = TestCompletable()
        val testObserver = TestObserver<Result<String>>()
        given(repositoryMock.updateName(MockUtils.any(), ArgumentMatchers.anyString())).willReturn(testCompletable)
        viewModel.setup(BigInteger.ZERO)

        viewModel.updateSafeName(newName).subscribe(testObserver)

        assertEquals(1, testCompletable.callCount)
        BDDMockito.then(repositoryMock).should().updateName(BigInteger.ZERO, cleanName)
        BDDMockito.then(repositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(DataResult(cleanName)).assertNoErrors()
    }

    @Test
    fun changeSafeNameBlankNAme() {
        val newName = ""
        val testObserver = TestObserver<Result<String>>()
        viewModel.setup(BigInteger.ZERO)

        viewModel.updateSafeName(newName).subscribe(testObserver)

        BDDMockito.then(repositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(ErrorResult(SimpleLocalizedException(R.string.error_blank_name.toString()))).assertNoErrors()
    }

    @Test
    fun changeSafeNameError() {
        val newName = "newName"
        val exception = Exception()
        val testObserver = TestObserver<Result<String>>()
        given(repositoryMock.updateName(MockUtils.any(), ArgumentMatchers.anyString())).willReturn(Completable.error(exception))
        viewModel.setup(BigInteger.ZERO)

        viewModel.updateSafeName(newName).subscribe(testObserver)

        BDDMockito.then(repositoryMock).should().updateName(BigInteger.ZERO, newName)
        BDDMockito.then(repositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(ErrorResult(exception)).assertNoErrors()
    }

    @Test
    fun deleteSafe() {
        val testCompletable = TestCompletable()
        val testObserver = TestObserver<Result<Unit>>()
        given(repositoryMock.remove(MockUtils.any())).willReturn(testCompletable)
        viewModel.setup(BigInteger.ZERO)

        viewModel.deleteSafe().subscribe(testObserver)

        assertEquals(1, testCompletable.callCount)
        BDDMockito.then(repositoryMock).should().remove(BigInteger.ZERO)
        BDDMockito.then(repositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(DataResult(Unit)).assertNoErrors()
    }

    @Test
    fun deleteSafeError() {
        val testObserver = TestObserver<Result<Unit>>()
        val exception = Exception()
        given(repositoryMock.remove(MockUtils.any())).willReturn(Completable.error(exception))
        viewModel.setup(BigInteger.ZERO)

        viewModel.deleteSafe().subscribe(testObserver)

        BDDMockito.then(repositoryMock).should().remove(BigInteger.ZERO)
        BDDMockito.then(repositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(ErrorResult(exception)).assertNoErrors()
    }

    @Test
    fun loadSafeName() {
        val testProcessor = PublishProcessor.create<Safe>()
        val testObserver = TestObserver<String>()
        given(repositoryMock.observeSafe(BigInteger.ZERO)).willReturn(testProcessor)
        viewModel.setup(BigInteger.ZERO)

        viewModel.loadSafeName().subscribe(testObserver)

        testProcessor.offer(Safe(BigInteger.ZERO, "Test Name"))
        testProcessor.offer(Safe(BigInteger.ZERO, "Test Name 2"))

        BDDMockito.then(repositoryMock).should().observeSafe(BigInteger.ZERO)
        BDDMockito.then(repositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult("Test Name")
    }

    @Test
    fun loadSafeNameError() {
        val testObserver = TestObserver<String>()
        given(repositoryMock.observeSafe(BigInteger.ZERO)).willReturn(Flowable.error(IllegalStateException()))
        viewModel.setup(BigInteger.ZERO)

        viewModel.loadSafeName().subscribe(testObserver)

        BDDMockito.then(repositoryMock).should().observeSafe(BigInteger.ZERO)
        BDDMockito.then(repositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertNoErrors().assertValue("")
    }

    @Test
    fun getSafeAddress() {
        viewModel.setup(BigInteger.ZERO)
        assertEquals(BigInteger.ZERO, viewModel.getSafeAddress())
    }
}
