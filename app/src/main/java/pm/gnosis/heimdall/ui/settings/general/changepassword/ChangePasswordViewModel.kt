package pm.gnosis.heimdall.ui.settings.general.changepassword

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.helpers.PasswordHelper
import pm.gnosis.heimdall.helpers.PasswordValidationCondition
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.svalinn.security.EncryptionManager
import javax.inject.Inject

class ChangePasswordViewModel @Inject constructor(
    private val encryptionManager: EncryptionManager
) : ChangePasswordContract() {
    override fun validatePassword(password: String): Single<Result<Collection<PasswordValidationCondition>>> =
        Single.fromCallable {
            PasswordHelper.Validator.validate(password)
        }
            .subscribeOn(Schedulers.io())
            .mapToResult()


    override fun validateRepeat(password: String, repeat: String): Single<Result<Boolean>> =
        Single.fromCallable {
            password == repeat
        }
            .subscribeOn(Schedulers.io())
            .mapToResult()

    override fun setPassword(currentPassword: String, newPassword: String, newPasswordRepeat: String) =
        encryptionManager.unlockWithPassword(currentPassword.toByteArray())
            .flatMap {
                if (it) {
                    val checkPasswords = PasswordHelper.Validator.validate(newPassword).any { !it.valid } || newPassword != newPasswordRepeat
                    if (checkPasswords) {
                        return@flatMap Single.just(State.ENTER_NEW_PASSWORD)
                    }
                    encryptionManager.setupPassword(
                        newPassword.toByteArray(),
                        currentPassword.toByteArray()
                    )
                        .map {
                            if (it) State.PASSWORD_CHANGED
                            else State.INVALID_PASSWORD
                        }
                } else
                    Single.just(State.INVALID_PASSWORD)
            }
            .mapToResult()
}
