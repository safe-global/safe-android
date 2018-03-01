package pm.gnosis.heimdall.ui.settings.security.changepassword

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
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.tests.utils.*

@RunWith(MockitoJUnitRunner::class)
class ChangePasswordViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var encryptionManagerMock: EncryptionManager

    private lateinit var viewModel: ChangePasswordViewModel

    @Before
    fun setUp() {
        contextMock.mockGetString()
        contextMock.mockGetStringWithArgs()
        viewModel = ChangePasswordViewModel(contextMock, encryptionManagerMock)
    }

    @Test
    fun setupPasswordTooShort() {
        val observer = createObserver()

        viewModel.setPassword("111111", "", "").subscribe(observer)

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        then(contextMock).should().getString(R.string.password_too_short, emptyArray<Any>())
        observer.assertResult(ErrorResult(SimpleLocalizedException(contextMock.getTestString(R.string.password_too_short))))
    }

    @Test
    fun setupPasswordNotSame() {
        val observer = createObserver()

        viewModel.setPassword("111111", "123456", "").subscribe(observer)

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        then(contextMock).should().getString(R.string.passwords_do_not_match, emptyArray<Any>())
        observer.assertResult(ErrorResult(SimpleLocalizedException(contextMock.getTestString(R.string.passwords_do_not_match))))
    }

    @Test
    fun setupPasswordException() {
        val observer = createObserver()
        val exception = Exception()
        given(encryptionManagerMock.setupPassword(MockUtils.any(), MockUtils.any())).willReturn(Single.error(exception))

        viewModel.setPassword("111111", "123456", "123456").subscribe(observer)

        then(encryptionManagerMock).should().setupPassword("123456".toByteArray(), "111111".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        then(contextMock).should().getString(R.string.password_error_saving)
        observer.assertResult(ErrorResult(SimpleLocalizedException(R.string.password_error_saving.toString())))
    }

    @Test
    fun setupPasswordNoSuccess() {
        val observer = createObserver()
        given(encryptionManagerMock.setupPassword(MockUtils.any(), MockUtils.any())).willReturn(Single.just(false))

        viewModel.setPassword("111111", "123456", "123456").subscribe(observer)

        then(encryptionManagerMock).should().setupPassword("123456".toByteArray(), "111111".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        then(contextMock).should().getString(R.string.password_error_saving)
        observer.assertResult(ErrorResult(SimpleLocalizedException(R.string.password_error_saving.toString())))
    }

    @Test
    fun setupPasswordSuccess() {
        val observer = createObserver()
        given(encryptionManagerMock.setupPassword(MockUtils.any(), MockUtils.any())).willReturn(Single.just(true))

        viewModel.setPassword("111111", "123456", "123456").subscribe(observer)

        then(encryptionManagerMock).should().setupPassword("123456".toByteArray(), "111111".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        observer.assertResult(DataResult(Unit))
    }

    private fun createObserver() = TestObserver.create<Result<Unit>>()
}
