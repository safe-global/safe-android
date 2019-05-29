package pm.gnosis.heimdall.ui.settings.general.changepassword

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import pm.gnosis.heimdall.helpers.PasswordHelper
import pm.gnosis.heimdall.helpers.PasswordValidationCondition
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.svalinn.security.EncryptionManager
import javax.inject.Inject

class ChangePasswordViewModel @Inject constructor(
    private val encryptionManager: EncryptionManager
) : ChangePasswordContract() {

    private val confirmSubject = PublishSubject.create<Unit>()
    private val viewState = PublishSubject.create<ViewState>()

    private var currentPassword: String? = null


    override fun validatePassword(password: String, repeat: String): Single<Result<Collection<PasswordValidationCondition>>> =
        Single.fromCallable {
            PasswordHelper.Validator.validate(password)
        }
            .doOnSuccess {
                val valid = it.all { it.valid } && password == repeat
                changeState(ViewState(State.ENTER_NEW_PASSWORD, valid))
            }
            .subscribeOn(Schedulers.io())
            .mapToResult()


    override fun validateRepeat(password: String, repeat: String): Single<Result<Boolean>> =
        Single.fromCallable {
            password == repeat
        }
            .doOnSubscribe {
                changeState(ViewState(State.ENTER_NEW_PASSWORD, password == repeat && PasswordHelper.Validator.validate(password).all { it.valid }))
            }
            .subscribeOn(Schedulers.io())
            .mapToResult()

    override fun confirmPassword(currentPassword: String): Single<Result<ViewState>> {
        return encryptionManager.unlockWithPassword(currentPassword.toByteArray())
            .map {
                if (it) {
                    this.currentPassword = currentPassword
                    val newViewState = ViewState(State.ENTER_NEW_PASSWORD, false)
                    changeState(newViewState)
                    newViewState

                } else {
                    val newViewState = ViewState(State.INVALID_PASSWORD, true)
                    changeState(newViewState)
                    newViewState
                }
            }
            .mapToResult()
    }

    override fun changePassword(newPassword: String, newPasswordRepeat: String): Single<Result<ViewState>> {
        val checkPasswords = PasswordHelper.Validator.validate(newPassword).any { !it.valid } || newPassword != newPasswordRepeat
        return if (checkPasswords) {
            val newViewState = ViewState(State.ENTER_NEW_PASSWORD, false)
            changeState(newViewState)
            Single.just(newViewState).mapToResult()
        } else {
            encryptionManager.setupPassword(
                newPassword.toByteArray(),
                currentPassword?.toByteArray()
            )
                .map {
                    val newViewState = if (it) ViewState(State.PASSWORD_CHANGED, true)
                    else ViewState(State.ERROR, true)
                    changeState(newViewState)
                    newViewState
                }
                .mapToResult()
        }
    }

    override fun confirmEvents(): Observable<Unit> {
        return confirmSubject
    }

    override fun confirm() {
        confirmSubject.onNext(Unit)
    }

    override fun state(): Observable<ViewState> {
        return viewState
    }

    private fun changeState(state: ViewState) {
        viewState.onNext(state)
    }
}
