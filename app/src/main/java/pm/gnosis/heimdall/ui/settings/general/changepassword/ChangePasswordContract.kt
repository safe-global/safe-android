package pm.gnosis.heimdall.ui.settings.general.changepassword

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.helpers.PasswordValidationCondition
import pm.gnosis.svalinn.common.utils.Result

abstract class ChangePasswordContract : ViewModel() {
    abstract fun validatePassword(password: String): Single<Result<Collection<PasswordValidationCondition>>>
    abstract fun validateRepeat(password: String, repeat: String): Single<Result<Boolean>>
    @Deprecated("used by ChangePasswordDialog")
    abstract fun setPassword(currentPassword: String, newPassword: String, newPasswordRepeat: String): Single<Result<State>>
    abstract fun confirmPassword(currentPassword: String): Single<Result<ViewState>>
    abstract fun changePassword(newPassword: String, newPasswordRepeat: String): Single<Result<ViewState>>

    abstract fun confirm()
    abstract fun confirmEvents(): Observable<Unit>
    abstract fun state(): Observable<ViewState>


    enum class State {
        ENTER_OLD_PASSWORD,
        INVALID_PASSWORD,
        ENTER_NEW_PASSWORD,
        PASSWORD_CHANGED,
        ERROR
    }

    data class ViewState(
        val state: State,
        val confirmEnabled: Boolean
    )
}
