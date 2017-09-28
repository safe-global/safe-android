package pm.gnosis.heimdall.ui.security

import android.content.Context
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule
import pm.gnosis.heimdall.test.utils.MockUtils
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import org.mockito.Mockito.`when` as given


@RunWith(MockitoJUnitRunner::class)
class SecurityViewModelTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var encryptionManager: EncryptionManager

    @Before
    fun setup() {
        given(context.getString(anyInt(), any())).thenReturn(TEST_STRING)
    }

    @Test
    fun checkStateUnlocked() {
        val viewModel = createViewModel()
        val observer = createObserver()

        given(encryptionManager.unlocked()).thenReturn(Single.just(true))
        viewModel.checkState().subscribe(observer)

        observer.assertNoErrors()
        observer.assertValue(Result(SecurityContract.State.UNLOCKED))
    }

    @Test
    fun checkStateLocked() {
        val viewModel = createViewModel()
        val observer = createObserver()

        given(encryptionManager.unlocked()).thenReturn(Single.just(false))
        given(encryptionManager.initialized()).thenReturn(Single.just(true))
        viewModel.checkState().subscribe(observer)

        observer.assertNoErrors()
        observer.assertValue(Result(SecurityContract.State.LOCKED))
    }

    @Test
    fun checkStateUninitialized() {
        val viewModel = createViewModel()
        val observer = createObserver()

        given(encryptionManager.unlocked()).thenReturn(Single.just(false))
        given(encryptionManager.initialized()).thenReturn(Single.just(false))
        viewModel.checkState().subscribe(observer)

        observer.assertNoErrors()
        observer.assertValue(Result(SecurityContract.State.UNINITIALIZED))
    }

    @Test
    fun checkStateError() {
        val viewModel = createViewModel()
        val observer = createObserver()

        val exception = IllegalStateException()
        given(encryptionManager.unlocked()).thenReturn(Single.error(exception))
        viewModel.checkState().subscribe(observer)

        observer.assertNoErrors()
        observer.assertValue(Result(exception))
    }

    @Test
    fun setupPinTooShort() {
        val viewModel = createViewModel()
        val observer = createObserver()

        viewModel.setupPin("", "").subscribe(observer)

        observer.assertNoErrors()
        observer.assertValue(Result(LocalizedException(TEST_STRING)))
        verify(context).getString(R.string.pin_too_short, emptyArray<Any>())
    }

    @Test
    fun setupPinNotSame() {
        val viewModel = createViewModel()
        val observer = createObserver()

        viewModel.setupPin("123456", "").subscribe(observer)

        observer.assertNoErrors()
        observer.assertValue(Result(LocalizedException(TEST_STRING)))
        verify(context).getString(R.string.pin_repeat_wrong, emptyArray<Any>())
    }

    @Test
    fun setupPinException() {
        val viewModel = createViewModel()
        val observer = createObserver()

        val exception = IllegalStateException()
        given(encryptionManager.setup(MockUtils.any(), MockUtils.any())).thenReturn(Single.error(exception))
        viewModel.setupPin("123456", "123456").subscribe(observer)

        observer.assertNoErrors()
        observer.assertValue(Result(exception))
    }

    @Test
    fun setupPinNoSuccess() {
        val viewModel = createViewModel()
        val observer = createObserver()

        given(encryptionManager.setup(MockUtils.any(), MockUtils.any())).thenReturn(Single.just(false))
        viewModel.setupPin("123456", "123456").subscribe(observer)

        observer.assertNoErrors()
        observer.assertValue(Result(LocalizedException(TEST_STRING)))
        verify(context).getString(R.string.pin_setup_failed, emptyArray<Any>())
    }

    @Test
    fun setupPinSuccess() {
        val viewModel = createViewModel()
        val observer = createObserver()

        given(encryptionManager.setup(MockUtils.any(), MockUtils.any())).thenReturn(Single.just(true))
        viewModel.setupPin("123456", "123456").subscribe(observer)

        observer.assertNoErrors()
        observer.assertValue(Result(SecurityContract.State.UNLOCKED))
    }

    @Test
    fun unlockPinException() {
        val viewModel = createViewModel()
        val observer = createObserver()

        val exception = IllegalStateException()
        given(encryptionManager.unlock(MockUtils.any())).thenReturn(Single.error(exception))
        viewModel.unlockPin("123456").subscribe(observer)

        observer.assertNoErrors()
        observer.assertValue(Result(exception))
    }

    @Test
    fun unlockPinNoSuccess() {
        val viewModel = createViewModel()
        val observer = createObserver()

        given(encryptionManager.unlock(MockUtils.any())).thenReturn(Single.just(false))
        viewModel.unlockPin("123456").subscribe(observer)

        observer.assertNoErrors()
        observer.assertValue(Result(LocalizedException(TEST_STRING)))
        verify(context).getString(R.string.error_wrong_credentials, emptyArray<Any>())
    }

    @Test
    fun unlockPinSuccess() {
        val viewModel = createViewModel()
        val observer = createObserver()

        given(encryptionManager.unlock(MockUtils.any())).thenReturn(Single.just(true))
        viewModel.unlockPin("123456").subscribe(observer)

        observer.assertNoErrors()
        observer.assertValue(Result(SecurityContract.State.UNLOCKED))
    }

    private fun createObserver() =
            TestObserver.create<Result<SecurityContract.State>>()

    private fun createViewModel() =
            SecurityViewModel(context, encryptionManager)

    companion object {
        const val TEST_STRING = "TEST"
    }

}