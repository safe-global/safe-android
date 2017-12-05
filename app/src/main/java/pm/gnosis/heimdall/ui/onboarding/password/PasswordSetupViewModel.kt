package pm.gnosis.heimdall.ui.onboarding.password

import android.content.Context
import io.reactivex.Observable
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import javax.inject.Inject

class PasswordSetupViewModel @Inject constructor(
        @ApplicationContext private val context: Context,
        private val encryptionManager: EncryptionManager
) : PasswordSetupContract() {
    private val errorHandler = SimpleLocalizedException.Handler.Builder(context)
            .add({ it is PasswordsDoNotMatchException }, R.string.passwords_do_not_match)
            .add({ it is PasswordTooShortException }, R.string.password_too_short)
            .add({ it is PasswordNotSavedException }, R.string.password_error_saving)
            .build()

    override fun setPassword(password: String, repeat: String) =
            Observable
                    .fromCallable {
                        if (password.length < 6) throw PasswordTooShortException()
                        if (password != repeat) throw PasswordsDoNotMatchException()
                        password
                    }
                    .flatMapSingle { encryptionManager.setupPassword(it.toByteArray()) }
                    .map { if (it) Unit else throw PasswordNotSavedException() }
                    .onErrorResumeNext { t: Throwable -> errorHandler.observable(t) }
                    .mapToResult()
}
