package pm.gnosis.heimdall.ui.security.unlock

import android.content.Context
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
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
class UnlockViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var encryptionManagerMock: EncryptionManager

    lateinit var viewModel: UnlockViewModel

    @Before
    fun setup() {
        viewModel = UnlockViewModel(contextMock, encryptionManagerMock)
        given(contextMock.getString(anyInt(), any())).willReturn(TEST_STRING)
    }

    @Test
    fun checkStateUnlocked() {
        val observer = createObserver()
        given(encryptionManagerMock.unlocked()).willReturn(Single.just(true))

        viewModel.checkState().subscribe(observer)

        then(encryptionManagerMock).should().unlocked()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertValue(DataResult(UnlockContract.State.UNLOCKED))
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
        observer.assertNoErrors().assertValue(DataResult(UnlockContract.State.LOCKED))
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
        observer.assertNoErrors().assertValue(DataResult(UnlockContract.State.UNINITIALIZED))
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
    fun unlockPinException() {
        val observer = createObserver()
        val exception = IllegalStateException()
        given(encryptionManagerMock.unlockWithPassword(MockUtils.any())).willReturn(Single.error(exception))

        viewModel.unlock("123456").subscribe(observer)

        then(encryptionManagerMock).should().unlockWithPassword("123456".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertValue(ErrorResult(exception))
    }

    @Test
    fun unlockPinNoSuccess() {
        val observer = createObserver()
        given(encryptionManagerMock.unlockWithPassword(MockUtils.any())).willReturn(Single.just(false))

        viewModel.unlock("123456").subscribe(observer)

        then(encryptionManagerMock).should().unlockWithPassword("123456".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertValue(ErrorResult(SimpleLocalizedException(TEST_STRING)))
        then(contextMock).should().getString(R.string.error_wrong_credentials, emptyArray<Any>())
    }

    @Test
    fun unlockPinSuccess() {
        val observer = createObserver()
        given(encryptionManagerMock.unlockWithPassword(MockUtils.any())).willReturn(Single.just(true))

        viewModel.unlock("123456").subscribe(observer)

        then(encryptionManagerMock).should().unlockWithPassword("123456".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertValue(DataResult(UnlockContract.State.UNLOCKED))
    }

    private fun createObserver() = TestObserver.create<Result<UnlockContract.State>>()

    companion object {
        const val TEST_STRING = "TEST"
    }
}
