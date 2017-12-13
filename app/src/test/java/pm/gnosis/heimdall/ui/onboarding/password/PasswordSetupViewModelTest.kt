package pm.gnosis.heimdall.ui.onboarding.password

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
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.mockGetString

@RunWith(MockitoJUnitRunner::class)
class PasswordSetupViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var encryptionManagerMock: EncryptionManager

    private lateinit var viewModel: PasswordSetupViewModel

    @Before
    fun setup() {
        contextMock.mockGetString()
        viewModel = PasswordSetupViewModel(contextMock, encryptionManagerMock)
    }

    @Test
    fun setupPasswordTooShort() {
        val observer = createObserver()

        viewModel.setPassword("", "").subscribe(observer)

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        then(contextMock).should().getString(R.string.password_too_short)
        observer.assertNoErrors()
                .assertValue { it is ErrorResult && it.error is SimpleLocalizedException }
    }

    @Test
    fun setupPasswordNotSame() {
        val observer = createObserver()

        viewModel.setPassword("123456", "").subscribe(observer)

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        then(contextMock).should().getString(R.string.passwords_do_not_match)
        observer.assertNoErrors()
                .assertValue { it is ErrorResult && it.error is SimpleLocalizedException }
    }

    @Test
    fun setupPasswordException() {
        val observer = createObserver()
        val exception = IllegalStateException()
        given(encryptionManagerMock.setupPassword(MockUtils.any(), MockUtils.any())).willReturn(Single.error(exception))

        viewModel.setPassword("123456", "123456").subscribe(observer)

        then(encryptionManagerMock).should().setupPassword("123456".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        then(contextMock).should().getString(R.string.password_error_saving)
        observer.assertNoErrors()
                .assertValue { it is ErrorResult && it.error is SimpleLocalizedException }
    }

    @Test
    fun setupPasswordNoSuccess() {
        val observer = createObserver()
        given(encryptionManagerMock.setupPassword(MockUtils.any(), MockUtils.any())).willReturn(Single.just(false))

        viewModel.setPassword("123456", "123456").subscribe(observer)

        then(encryptionManagerMock).should().setupPassword("123456".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors()
                .assertValue { it is ErrorResult && it.error is SimpleLocalizedException }
    }

    @Test
    fun setupPasswordSuccess() {
        val observer = createObserver()
        given(encryptionManagerMock.setupPassword(MockUtils.any(), MockUtils.any())).willReturn(Single.just(true))

        viewModel.setPassword("123456", "123456").subscribe(observer)

        then(encryptionManagerMock).should().setupPassword("123456".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertValue(DataResult(Unit))
    }

    private fun createObserver() = TestObserver.create<Result<Unit>>()
}
