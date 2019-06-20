package pm.gnosis.heimdall.ui.onboarding.password

import android.content.Intent
import androidx.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.heimdall.helpers.PasswordValidationCondition
import pm.gnosis.svalinn.common.utils.Result

abstract class PasswordSetupContract : ViewModel() {
    abstract fun createAccount(passwordHash: ByteArray, repeat: String): Single<Result<Intent>>
    abstract fun passwordToHash(password: String): Single<Result<ByteArray>>
    abstract fun validatePassword(password: String): Single<Result<Collection<PasswordValidationCondition>>>
    abstract fun isSamePassword(passwordHash: ByteArray, repeat: String): Single<Result<Boolean>>
}

data class PasswordInvalidException(val reason: PasswordValidation) : IllegalArgumentException()
sealed class PasswordValidation
data class PasswordNotLongEnough(val numberOfCharacters: Int, val required: Int) : PasswordValidation()
object PasswordsNotEqual : PasswordValidation()
