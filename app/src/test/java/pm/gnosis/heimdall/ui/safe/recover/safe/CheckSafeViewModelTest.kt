package pm.gnosis.heimdall.ui.safe.recover.safe

import android.content.Context
import androidx.room.EmptyResultSetException
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.safe.recover.safe.CheckSafeContract.CheckResult
import pm.gnosis.heimdall.utils.SafeContractUtils
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.mockGetString
import pm.gnosis.utils.asEthereumAddress
import java.net.UnknownHostException

@RunWith(MockitoJUnitRunner::class)
class CheckSafeViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var safeRepoMock: GnosisSafeRepository

    private lateinit var viewModel: CheckSafeViewModel

    @Before
    fun setUp() {
        viewModel = CheckSafeViewModel(contextMock, safeRepoMock)
    }

    @Test
    fun checkSafeAddressEmpty() {
        val observer = TestObserver<Result<CheckResult>>()
        viewModel.checkSafe("").subscribe(observer)
        observer.assertResult(DataResult(CheckResult.INVALID_SAFE))

        then(safeRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkSafeAddressInvalid() {
        val observer = TestObserver<Result<CheckResult>>()
        viewModel.checkSafe("InvalidAddress").subscribe(observer)
        observer.assertResult(DataResult(CheckResult.INVALID_SAFE))

        then(safeRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkSafeTooShort() {
        val observer = TestObserver<Result<CheckResult>>()
        viewModel.checkSafe("C2AC20b3Bb950C087f18a458DB68271325a48132").subscribe(observer)
        observer.assertResult(DataResult(CheckResult.INVALID_SAFE))

        then(safeRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkSafeAlreadyExists() {
        contextMock.mockGetString()
        given(safeRepoMock.loadAbstractSafe(MockUtils.any())).willReturn(Single.just(Safe(TEST_SAFE)))

        val observer = TestObserver<Result<CheckResult>>()
        viewModel.checkSafe("0x1f81FFF89Bd57811983a35650296681f99C65C7e").subscribe(observer)
        observer.assertResult(ErrorResult(SimpleLocalizedException(R.string.safe_already_exists.toString())))

        then(safeRepoMock).should().loadAbstractSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun checkSafeUnknownErrorCheckingExistence() {
        contextMock.mockGetString()
        val error = IllegalStateException()
        given(safeRepoMock.loadAbstractSafe(MockUtils.any())).willReturn(Single.error(error))

        val observer = TestObserver<Result<CheckResult>>()
        viewModel.checkSafe("0xb36574155395D41b92664e7A215103262a14244A").subscribe(observer)
        observer.assertResult(ErrorResult(error))

        then(safeRepoMock).should().loadAbstractSafe(TEST_RECOVERING_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun checkSafeNetworkError() {
        contextMock.mockGetString()
        given(safeRepoMock.loadAbstractSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepoMock.checkSafe(MockUtils.any())).willReturn(Observable.error(UnknownHostException()))

        val observer = TestObserver<Result<CheckResult>>()
        viewModel.checkSafe("0x1f81fff89Bd57811983a35650296681f99C65C7E").subscribe(observer)
        observer.assertResult(ErrorResult(SimpleLocalizedException(R.string.error_check_internet_connection.toString())))

        then(safeRepoMock).should().loadAbstractSafe(TEST_SAFE)
        then(safeRepoMock).should().checkSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun checkSafeInvalidSafe() {
        contextMock.mockGetString()
        given(safeRepoMock.loadAbstractSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepoMock.checkSafe(MockUtils.any())).willReturn(Observable.just(null to false))

        val observer = TestObserver<Result<CheckResult>>()
        viewModel.checkSafe("0x1f81fff89Bd57811983a35650296681f99C65C7E").subscribe(observer)
        observer.assertResult(DataResult(CheckResult.INVALID_SAFE))

        then(safeRepoMock).should().loadAbstractSafe(TEST_SAFE)
        then(safeRepoMock).should().checkSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun checkSafeValidSafeWithExtension() {
        contextMock.mockGetString()
        given(safeRepoMock.loadAbstractSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepoMock.checkSafe(MockUtils.any())).willReturn(Observable.just(SafeContractUtils.currentMasterCopy() to true))

        val observer = TestObserver<Result<CheckResult>>()
        viewModel.checkSafe("0x1f81fff89Bd57811983a35650296681f99C65C7E").subscribe(observer)
        observer.assertResult(DataResult(CheckResult.VALID_SAFE_WITH_EXTENSION))

        then(safeRepoMock).should().loadAbstractSafe(TEST_SAFE)
        then(safeRepoMock).should().checkSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun checkSafeValidSafeWithoutExtension() {
        contextMock.mockGetString()
        given(safeRepoMock.loadAbstractSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepoMock.checkSafe(MockUtils.any())).willReturn(Observable.just(SafeContractUtils.currentMasterCopy() to false))

        val observer = TestObserver<Result<CheckResult>>()
        viewModel.checkSafe("0x1f81fff89Bd57811983a35650296681f99C65C7E").subscribe(observer)
        observer.assertResult(DataResult(CheckResult.VALID_SAFE_WITHOUT_EXTENSION))

        then(safeRepoMock).should().loadAbstractSafe(TEST_SAFE)
        then(safeRepoMock).should().checkSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
    }

    companion object {
        private val TEST_SAFE = "0x1f81FFF89Bd57811983a35650296681f99C65C7E".asEthereumAddress()!!
        private val TEST_RECOVERING_SAFE = "0xb36574155395D41b92664e7A215103262a14244A".asEthereumAddress()!!
    }
}
