package pm.gnosis.heimdall.ui.settings.security.changepassword

import android.content.Context
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.svalinn.security.EncryptionManager
import javax.inject.Inject

class ChangePasswordViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionManager: EncryptionManager
) : ChangePasswordContract() {
    override fun setPassword(currentPassword: String, newPassword: String, newPasswordRepeat: String) =
        Observable
            .fromCallable {
                SimpleLocalizedException.assert(newPassword.length > 5, context, R.string.password_too_short)
                SimpleLocalizedException.assert(newPassword == newPasswordRepeat, context, R.string.passwords_do_not_match)
                newPassword
            }
            .flatMapSingle {
                encryptionManager.setupPassword(it.toByteArray(), currentPassword.toByteArray())
                    .map { if (it) Unit else throw Exception() }
                    .onErrorResumeNext { _: Throwable -> Single.error(SimpleLocalizedException(context.getString(R.string.password_error_saving))) }
            }
            .mapToResult()
}
