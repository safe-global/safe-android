package pm.gnosis.heimdall.ui.security

import android.content.Context
import android.support.annotation.StringRes
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.heimdall.ui.security.SecurityContract.Presenter
import pm.gnosis.heimdall.ui.security.SecurityContract.ViewState
import timber.log.Timber
import javax.inject.Inject

@ForView
class SecurityPresenter @Inject constructor(@ApplicationContext val context: Context, val encryptionManager: EncryptionManager) : Presenter {

    private val stateRelay = BehaviorRelay.create<State>()
    private val notificationRelay = PublishRelay.create<String>()

    private val disposabled = CompositeDisposable()

    override fun start() {
        disposabled += encryptionManager.unlocked()
                .doOnSubscribe { stateRelay.accept(State.LOADING) }
                .flatMap({
                    if (it)
                        Single.just(State.UNLOCKED)
                    else
                        encryptionManager.initialized().map {
                            if (it)
                                State.LOCKED
                            else
                                State.UNINITIALIZED
                        }
                }).subscribe(stateRelay::accept, this::propagateFatalError)
    }

    private fun propagateFatalError(throwable: Throwable) {
        Timber.d(throwable)
        stateRelay.accept(State.ERROR)
    }


    private fun showErrorNotification(@StringRes messageResId: Int) {
        notificationRelay.accept(context.getString(messageResId))
    }

    override fun unlockPin(pin: String) {
        disposabled += encryptionManager.unlock(pin.toByteArray())
                .doOnSubscribe { stateRelay.accept(State.LOADING) }
                .map {
                    if (it) {
                        State.UNLOCKED
                    } else {
                        throw IllegalArgumentException()
                    }
                }
                .subscribe(stateRelay::accept, { showErrorNotification(R.string.error_wrong_credentials) })
    }

    override fun setupPin(pin: String, repeat: String) {
        if (pin != repeat) {
            showErrorNotification(R.string.pin_repeat_wrong)
            return
        }
        disposabled += encryptionManager.setup(pin.toByteArray())
                .doOnSubscribe { stateRelay.accept(State.LOADING) }
                .map {
                    if (it) {
                        // User should unlock screen after setup
                        State.LOCKED
                    } else {
                        throw IllegalArgumentException()
                    }
                }
                .subscribe(stateRelay::accept, { showErrorNotification(R.string.error_try_again) })
    }

    override fun observeViewState(): Observable<SecurityContract.ViewState> {
        return stateRelay.scan<SecurityContract.ViewState>(ViewState.initial(), { (securityState), state ->
            when (state) {
                State.LOADING -> ViewState(securityState, true)
                State.ERROR -> ViewState.error()
                State.UNINITIALIZED -> ViewState.uninitialized()
                State.LOCKED -> ViewState.locked()
                State.UNLOCKED -> ViewState.unlocked()
            }
        })
    }

    override fun observeNotifications(): Observable<String> {
        return notificationRelay
    }

    override fun stop() {
        disposabled.clear()
    }

    private enum class State {
        LOADING,
        ERROR,
        UNINITIALIZED,
        LOCKED,
        UNLOCKED,
    }

}