package pm.gnosis.heimdall.ui.settings.general.changepassword

import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.helpers.PasswordValidationCondition
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils

@RunWith(MockitoJUnitRunner::class)
class ChangePasswordViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var encryptionManagerMock: EncryptionManager

    private lateinit var viewModel: ChangePasswordViewModel

    @Before
    fun setUp() {
        viewModel = ChangePasswordViewModel(encryptionManagerMock)
    }

    @Test
    fun validatePasswordValid() {
        val observer = TestObserver<Result<Collection<PasswordValidationCondition>>>()
        viewModel.validatePassword("qwe123qwe").subscribe(observer)
        observer.assertResult(
            DataResult(
                listOf(
                    PasswordValidationCondition.MinimumCharacters(true),
                    PasswordValidationCondition.OneNumberOneLetter(true),
                    PasswordValidationCondition.NonIdenticalCharacters(true)
                )
            )
        )
    }

    @Test
    fun validatePasswordInvalid() {
        val observer = TestObserver<Result<Collection<PasswordValidationCondition>>>()
        viewModel.validatePassword("").subscribe(observer)
        observer.assertResult(
            DataResult(
                listOf(
                    PasswordValidationCondition.MinimumCharacters(false),
                    PasswordValidationCondition.OneNumberOneLetter(false),
                    PasswordValidationCondition.NonIdenticalCharacters(false)
                )
            )
        )
    }

    @Test
    fun validateRepeatValid() {
        val observer = TestObserver<Result<Boolean>>()
        viewModel.validateRepeat("qwe123qwe", "qwe123qwe").subscribe(observer)
        observer.assertResult(DataResult(true))
    }

    @Test
    fun validateRepeatInvalid() {
        val observer = TestObserver<Result<Boolean>>()
        viewModel.validateRepeat("", "12e").subscribe(observer)
        observer.assertResult(DataResult(false))
    }

    @Test
    fun setPasswordWrongPassword() {
        given(encryptionManagerMock.unlockWithPassword(MockUtils.any())).willReturn(Single.just(false))
        val observer = TestObserver<Result<ChangePasswordContract.State>>()
        viewModel.setPassword("wqe", "qwe123qwe", "qwe123qwe").subscribe(observer)
        observer.assertResult(DataResult(ChangePasswordContract.State.INVALID_PASSWORD))
    }

    @Test
    fun setPasswordPasswordTooWeak() {
        given(encryptionManagerMock.unlockWithPassword(MockUtils.any())).willReturn(Single.just(true))
        val observer = TestObserver<Result<ChangePasswordContract.State>>()
        viewModel.setPassword("wqe", "", "").subscribe(observer)
        observer.assertResult(DataResult(ChangePasswordContract.State.ENTER_NEW_PASSWORD))
    }

    @Test
    fun setPasswordNoSamePassword() {
        given(encryptionManagerMock.unlockWithPassword(MockUtils.any())).willReturn(Single.just(true))
        val observer = TestObserver<Result<ChangePasswordContract.State>>()
        viewModel.setPassword("wqe", "qwe123qwe", "qwe123qwe2").subscribe(observer)
        observer.assertResult(DataResult(ChangePasswordContract.State.ENTER_NEW_PASSWORD))
    }

    @Test
    fun setPasswordCouldNotSet() {
        given(encryptionManagerMock.unlockWithPassword(MockUtils.any())).willReturn(Single.just(true))
        given(encryptionManagerMock.setupPassword(MockUtils.any(), MockUtils.any())).willReturn(Single.just(false))
        val observer = TestObserver<Result<ChangePasswordContract.State>>()
        viewModel.setPassword("wqe", "qwe123qwe", "qwe123qwe").subscribe(observer)
        observer.assertResult(DataResult(ChangePasswordContract.State.INVALID_PASSWORD))
    }

    @Test
    fun setPassword() {
        given(encryptionManagerMock.unlockWithPassword(MockUtils.any())).willReturn(Single.just(true))
        given(encryptionManagerMock.setupPassword(MockUtils.any(), MockUtils.any())).willReturn(Single.just(true))
        val observer = TestObserver<Result<ChangePasswordContract.State>>()
        viewModel.setPassword("wqe", "qwe123qwe", "qwe123qwe").subscribe(observer)
        observer.assertResult(DataResult(ChangePasswordContract.State.PASSWORD_CHANGED))
    }

    @Test
    fun setPasswordErrorUnlock() {
        val error = IllegalArgumentException()
        given(encryptionManagerMock.unlockWithPassword(MockUtils.any())).willReturn(Single.error(error))
        val observer = TestObserver<Result<ChangePasswordContract.State>>()
        viewModel.setPassword("wqe", "qwe123qwe", "qwe123qwe").subscribe(observer)
        observer.assertResult(ErrorResult(error))
    }

    @Test
    fun setPasswordErrorSet() {
        val error = IllegalArgumentException()
        given(encryptionManagerMock.unlockWithPassword(MockUtils.any())).willReturn(Single.just(true))
        given(encryptionManagerMock.setupPassword(MockUtils.any(), MockUtils.any())).willReturn(Single.error(error))
        val observer = TestObserver<Result<ChangePasswordContract.State>>()
        viewModel.setPassword("wqe", "qwe123qwe", "qwe123qwe").subscribe(observer)
        observer.assertResult(ErrorResult(error))
    }
}
