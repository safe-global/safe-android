package pm.gnosis.heimdall.ui.onboarding.password

import android.content.Context
import io.reactivex.Single
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.svalinn.common.di.ApplicationContext
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.svalinn.security.EncryptionManager
import javax.inject.Inject

class PasswordSetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionManager: EncryptionManager
) : PasswordSetupContract() {
    override fun isPasswordValid(password: String): PasswordValidation {
        return if (password.length > MIN_CHARS) PasswordValid(password) else PasswordNotLongEnough(password.length, MIN_CHARS)
    }

    override fun setPassword(password: String, repeat: String) =
        Single.fromCallable {
            if (password != repeat) throw PasswordInvalidException(PasswordsNotEqual())
            // This should never happen since it was validated in the previous screen
            isPasswordValid(password).let { validation -> if (validation !is PasswordValid) throw PasswordInvalidException(validation) }
            password
        }.flatMap {
            encryptionManager.setupPassword(it.toByteArray())
                .map { if (it) Unit else throw Exception() }
                .onErrorResumeNext { _: Throwable -> Single.error(SimpleLocalizedException(context.getString(R.string.password_error_saving))) }
        }.mapToResult()

    companion object {
        private const val MIN_CHARS = 5
    }
}
