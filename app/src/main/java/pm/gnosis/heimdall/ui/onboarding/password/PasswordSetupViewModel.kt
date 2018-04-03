package pm.gnosis.heimdall.ui.onboarding.password

import android.content.Context
import io.reactivex.Single
import pm.gnosis.crypto.utils.Sha3Utils
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
        return if (password.length >= MIN_CHARS) PasswordValid(password) else PasswordNotLongEnough(password.length, MIN_CHARS)
    }

    override fun setPassword(passwordHash: ByteArray, repeat: String) =
        Single.fromCallable {
            if (!Sha3Utils.keccak(repeat.toByteArray()).contentEquals(passwordHash)) throw PasswordInvalidException(PasswordsNotEqual())
            // This should never happen since it was validated in the previous screen
            isPasswordValid(repeat).let { validation -> if (validation !is PasswordValid) throw PasswordInvalidException(validation) }
            repeat.toByteArray()
        }.flatMap {
            encryptionManager.setupPassword(it)
                .map { if (it) Unit else throw Exception() }
                .onErrorResumeNext { _: Throwable -> Single.error(SimpleLocalizedException(context.getString(R.string.password_error_saving))) }
        }.mapToResult()

    companion object {
        private const val MIN_CHARS = 6
    }
}
