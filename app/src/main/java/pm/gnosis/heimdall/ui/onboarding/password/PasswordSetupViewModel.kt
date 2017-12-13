package pm.gnosis.heimdall.ui.onboarding.password

import android.content.Context
import io.reactivex.Observable
import io.reactivex.Single
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
    override fun setPassword(password: String, repeat: String) =
            Observable
                    .fromCallable {
                        if (password.length < 6) throw SimpleLocalizedException(context.getString(R.string.password_too_short))
                        if (password != repeat) throw SimpleLocalizedException(context.getString(R.string.passwords_do_not_match))
                        password
                    }
                    .flatMapSingle {
                        encryptionManager.setupPassword(it.toByteArray())
                                .map { if (it) Unit else throw Exception() }
                                .onErrorResumeNext { _: Throwable -> Single.error(SimpleLocalizedException(context.getString(R.string.password_error_saving))) }
                    }
                    .mapToResult()
}
