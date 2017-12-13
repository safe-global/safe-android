package pm.gnosis.heimdall.ui.security

import android.content.Context
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils

@RunWith(MockitoJUnitRunner::class)
class SecurityViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var encryptionManagerMock: EncryptionManager

    lateinit var viewModel: SecurityViewModel

    @Before
    fun setup() {
        viewModel = SecurityViewModel(contextMock, encryptionManagerMock)
        given(contextMock.getString(anyInt(), any())).willReturn(TEST_STRING)
    }

    @Test
    fun checkStateUnlocked() {
        val observer = createObserver()
        given(encryptionManagerMock.unlocked()).willReturn(Single.just(true))

        viewModel.checkState().subscribe(observer)

        then(encryptionManagerMock).should().unlocked()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertValue(DataResult(SecurityContract.State.UNLOCKED))
    }

    @Test
    fun checkStateLocked() {
        val observer = createObserver()
        given(encryptionManagerMock.unlocked()).willReturn(Single.just(false))
        given(encryptionManagerMock.initialized()).willReturn(Single.just(true))

        viewModel.checkState().subscribe(observer)

        then(encryptionManagerMock).should().unlocked()
        then(encryptionManagerMock).should().initialized()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertValue(DataResult(SecurityContract.State.LOCKED))
    }

    @Test
    fun checkStateUninitialized() {
        val observer = createObserver()
        given(encryptionManagerMock.unlocked()).willReturn(Single.just(false))
        given(encryptionManagerMock.initialized()).willReturn(Single.just(false))

        viewModel.checkState().subscribe(observer)

        then(encryptionManagerMock).should().unlocked()
        then(encryptionManagerMock).should().initialized()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertValue(DataResult(SecurityContract.State.UNINITIALIZED))
    }

    @Test
    fun checkStateError() {
        val observer = createObserver()
        val exception = IllegalStateException()
        given(encryptionManagerMock.unlocked()).willReturn(Single.error(exception))

        viewModel.checkState().subscribe(observer)

        then(encryptionManagerMock).should().unlocked()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertValue(ErrorResult(exception))
    }

    @Test
    fun setupPinTooShort() {
        val observer = createObserver()

        viewModel.setupPin("", "").subscribe(observer)

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        observer.assertNoErrors().assertValue(ErrorResult(SimpleLocalizedException(TEST_STRING)))
        then(contextMock).should().getString(R.string.pin_too_short, emptyArray<Any>())
    }

    @Test
    fun setupPinNotSame() {
        val observer = createObserver()

        viewModel.setupPin("123456", "").subscribe(observer)

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        observer.assertNoErrors().assertValue(ErrorResult(SimpleLocalizedException(TEST_STRING)))
        then(contextMock).should().getString(R.string.pin_repeat_wrong, emptyArray<Any>())
    }

    @Test
    fun setupPinException() {
        val observer = createObserver()
        val exception = IllegalStateException()
        given(encryptionManagerMock.setupPassword(MockUtils.any(), MockUtils.any())).willReturn(Single.error(exception))

        viewModel.setupPin("123456", "123456").subscribe(observer)

        then(encryptionManagerMock).should().setupPassword("123456".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertValue(ErrorResult(exception))
    }

    @Test
    fun setupPinNoSuccess() {
        val observer = createObserver()
        given(encryptionManagerMock.setupPassword(MockUtils.any(), MockUtils.any())).willReturn(Single.just(false))

        viewModel.setupPin("123456", "123456").subscribe(observer)

        then(encryptionManagerMock).should().setupPassword("123456".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertValue(ErrorResult(SimpleLocalizedException(TEST_STRING)))
        then(contextMock).should().getString(R.string.pin_setup_failed, emptyArray<Any>())
    }

    @Test
    fun setupPinSuccess() {
        val observer = createObserver()
        given(encryptionManagerMock.setupPassword(MockUtils.any(), MockUtils.any())).willReturn(Single.just(true))

        viewModel.setupPin("123456", "123456").subscribe(observer)

        then(encryptionManagerMock).should().setupPassword("123456".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertValue(DataResult(SecurityContract.State.UNLOCKED))
    }

    @Test
    fun unlockPinException() {
        val observer = createObserver()
        val exception = IllegalStateException()
        given(encryptionManagerMock.unlockWithPassword(MockUtils.any())).willReturn(Single.error(exception))

        viewModel.unlockPin("123456").subscribe(observer)

        then(encryptionManagerMock).should().unlockWithPassword("123456".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertValue(ErrorResult(exception))
    }

    @Test
    fun unlockPinNoSuccess() {
        val observer = createObserver()
        given(encryptionManagerMock.unlockWithPassword(MockUtils.any())).willReturn(Single.just(false))

        viewModel.unlockPin("123456").subscribe(observer)

        then(encryptionManagerMock).should().unlockWithPassword("123456".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertValue(ErrorResult(SimpleLocalizedException(TEST_STRING)))
        then(contextMock).should().getString(R.string.error_wrong_credentials, emptyArray<Any>())
    }

    @Test
    fun unlockPinSuccess() {
        val observer = createObserver()
        given(encryptionManagerMock.unlockWithPassword(MockUtils.any())).willReturn(Single.just(true))

        viewModel.unlockPin("123456").subscribe(observer)

        then(encryptionManagerMock).should().unlockWithPassword("123456".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertValue(DataResult(SecurityContract.State.UNLOCKED))
    }

    private fun createObserver() = TestObserver.create<Result<SecurityContract.State>>()

    companion object {
        const val TEST_STRING = "TEST"
    }
}
