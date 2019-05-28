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
    private var _viewState = ViewState(State.ENTER_OLD_PASSWORD, true)

    private var currentPassword: String? = null


    override fun validatePassword(password: String): Single<Result<Collection<PasswordValidationCondition>>> =
        Single.fromCallable {
            PasswordHelper.Validator.validate(password)
        }
            .doOnSuccess {
                val valid = it.all { it.valid }
                if (!valid)
                _viewState = ViewState(State.ENTER_NEW_PASSWORD, valid)
                changeState(_viewState)
            }
            .subscribeOn(Schedulers.io())
            .mapToResult()


    override fun validateRepeat(password: String, repeat: String): Single<Result<Boolean>> =
        Single.fromCallable {
            password == repeat
        }
            .doOnSubscribe {
                if (_viewState.state != State.ENTER_OLD_PASSWORD) {
                    _viewState = ViewState(State.ENTER_NEW_PASSWORD, password == repeat && password.isNotEmpty())
                    changeState(_viewState)
                }
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

    override fun confirmPassword(currentPassword: String): Single<Result<ViewState>> {
        return encryptionManager.unlockWithPassword(currentPassword.toByteArray())
            .map {
                if (it) {
                    this.currentPassword = currentPassword
                    _viewState = ViewState(State.ENTER_NEW_PASSWORD, false)
                    changeState(_viewState)
                    _viewState

                } else {
                    _viewState = ViewState(State.INVALID_PASSWORD, true)
                    changeState(_viewState)
                    _viewState
                }
            }
            .mapToResult()
    }

    override fun changePassword(newPassword: String, newPasswordRepeat: String): Single<Result<ViewState>> {
        val checkPasswords = PasswordHelper.Validator.validate(newPassword).any { !it.valid } || newPassword != newPasswordRepeat
        return if (checkPasswords) {
            _viewState = ViewState(State.ENTER_NEW_PASSWORD, false)
            changeState(_viewState)
            Single.just(_viewState).mapToResult()
        } else {
            encryptionManager.setupPassword(
                newPassword.toByteArray(),
                currentPassword?.toByteArray()
            )
                .map {
                    _viewState = if (it) ViewState(State.PASSWORD_CHANGED, true)
                    else ViewState(State.INVALID_PASSWORD, true)
                    changeState(_viewState)
                    _viewState
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
