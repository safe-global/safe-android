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
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.security.EncryptionManager
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
    fun setUp() {
        viewModel = PasswordSetupViewModel(contextMock, encryptionManagerMock)
    }

    @Test
    fun setupPasswordTooShort() {
        val observer = createObserver()
        val password = "11111"
        val passwordHash = Sha3Utils.keccak(password.toByteArray())

        viewModel.setPassword(passwordHash, password).subscribe(observer)

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        observer.assertResult(ErrorResult(PasswordInvalidException(PasswordNotLongEnough(password.length, MIN_CHARS))))
    }

    @Test
    fun setupPasswordNotSame() {
        val observer = createObserver()

        viewModel.setPassword(Sha3Utils.keccak("111111".toByteArray()), "123456").subscribe(observer)

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        observer.assertValue { it is ErrorResult && it.error is PasswordInvalidException && (it.error as PasswordInvalidException).reason is PasswordsNotEqual }
    }

    @Test
    fun setupPasswordException() {
        contextMock.mockGetString()
        val observer = createObserver()
        val exception = Exception()
        given(encryptionManagerMock.setupPassword(MockUtils.any(), MockUtils.any())).willReturn(Single.error(exception))

        viewModel.setPassword(Sha3Utils.keccak("111111".toByteArray()), "111111").subscribe(observer)

        then(encryptionManagerMock).should().setupPassword("111111".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        then(contextMock).should().getString(R.string.password_error_saving)
        observer.assertResult(ErrorResult(SimpleLocalizedException(R.string.password_error_saving.toString())))
    }

    @Test
    fun setupPasswordNoSuccess() {
        contextMock.mockGetString()
        val observer = createObserver()
        given(encryptionManagerMock.setupPassword(MockUtils.any(), MockUtils.any())).willReturn(Single.just(false))

        viewModel.setPassword(Sha3Utils.keccak("111111".toByteArray()), "111111").subscribe(observer)

        then(encryptionManagerMock).should().setupPassword("111111".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        then(contextMock).should().getString(R.string.password_error_saving)
        observer.assertResult(ErrorResult(SimpleLocalizedException(R.string.password_error_saving.toString())))
    }

    @Test
    fun setupPasswordSuccess() {
        val observer = createObserver()
        given(encryptionManagerMock.setupPassword(MockUtils.any(), MockUtils.any())).willReturn(Single.just(true))

        viewModel.setPassword(Sha3Utils.keccak("111111".toByteArray()), "111111").subscribe(observer)

        then(encryptionManagerMock).should().setupPassword("111111".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        observer.assertResult(DataResult(Unit))
    }

    private fun createObserver() = TestObserver.create<Result<Unit>>()

    companion object {
        private const val MIN_CHARS = 6
    }
}
