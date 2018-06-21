package pm.gnosis.heimdall.ui.settings.general.changepassword

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.helpers.PasswordValidationCondition
import pm.gnosis.svalinn.common.utils.Result

abstract class ChangePasswordContract : ViewModel() {
    abstract fun validatePassword(password: String): Single<Result<Collection<PasswordValidationCondition>>>
    abstract fun validateRepeat(password: String, repeat: String): Single<Result<Boolean>>
    abstract fun setPassword(currentPassword: String, newPassword: String, newPasswordRepeat: String): Single<Result<State>>

    enum class State {
        INVALID_PASSWORD,
        ENTER_NEW_PASSWORD,
        PASSWORD_CHANGED
    }
}
