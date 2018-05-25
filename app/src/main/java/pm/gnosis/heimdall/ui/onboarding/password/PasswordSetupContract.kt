package pm.gnosis.heimdall.ui.onboarding.password

import android.arch.lifecycle.ViewModel
import android.content.Intent
import io.reactivex.Single
import pm.gnosis.svalinn.common.utils.Result

abstract class PasswordSetupContract : ViewModel() {
    abstract fun createAccount(passwordHash: ByteArray, repeat: String): Single<Result<Intent>>
    abstract fun passwordToHash(password: String): Single<Result<ByteArray>>
    abstract fun validatePassword(password: String): Single<Result<List<Pair<PasswordValidationCondition, Boolean>>>>
    abstract fun isSamePassword(passwordHash: ByteArray, repeat: String): Single<Result<Boolean>>
}

data class PasswordInvalidException(val reason: PasswordValidation) : IllegalArgumentException()
sealed class PasswordValidation
data class PasswordNotLongEnough(val numberOfCharacters: Int, val required: Int) : PasswordValidation()
class PasswordsNotEqual : PasswordValidation()

enum class PasswordValidationCondition {
    NON_IDENTICAL_CHARACTERS,
    MINIMUM_CHARACTERS,
    ONE_NUMBER_ONE_LETTER
}
