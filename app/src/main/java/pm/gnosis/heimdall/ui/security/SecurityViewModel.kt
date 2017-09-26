package pm.gnosis.heimdall.ui.security

import android.content.Context
import android.support.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.heimdall.ui.base.BaseContract
import pm.gnosis.heimdall.ui.base.buildTransformer
import pm.gnosis.heimdall.ui.security.SecurityContract.ViewState
import timber.log.Timber
import javax.inject.Inject


@ForView
class SecurityViewModel @Inject constructor(@ApplicationContext val context: Context, val encryptionManager: EncryptionManager) :
        SecurityContract.ViewModel(), BaseContract.TransformerViewModel.Builder<BaseContract.UiEvent, SecurityViewModel.State, ViewState> {

    private fun initialData() =
            encryptionManager.unlocked()
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
                    }).toObservable()
                    .onErrorReturn { t ->
                        Timber.d(t)
                        State.FATAL_ERROR
                    }
                    .startWith(State.LOADING)

    override fun transformer(): ObservableTransformer<BaseContract.UiEvent, ViewState> =
            buildTransformer(initialData())

    override fun initialViewState() = ViewState.initial()

    override fun updateViewState(currentState: ViewState, data: State) =
            when (data) {
                State.LOADING -> currentState.copy(loading = true)
                State.FATAL_ERROR -> ViewState.error()

                State.UNINITIALIZED -> ViewState.uninitialized()
                State.LOCKED -> ViewState.locked()
                State.UNLOCKED -> ViewState.unlocked()

                State.CHECK_PIN_NOTIFICATION -> currentState.copy(notification = notification(R.string.error_wrong_credentials), loading = false)
                State.NOT_SAME_PINS_NOTIFICATION -> currentState.copy(notification = notification(R.string.pin_repeat_wrong), loading = false)
            }

    override fun handleEvent(event: BaseContract.UiEvent) =
            when (event) {
                is SecurityContract.SetupPin -> setupPin(event)
                is SecurityContract.Unlock -> unlockPin(event)
                else -> Observable.empty()
            }

    private fun notification(@StringRes messageResId: Int): SecurityContract.Notification {
        return SecurityContract.Notification(context.getString(messageResId))
    }

    private fun unlockPin(data: SecurityContract.Unlock): Observable<State> {
        return encryptionManager.unlock(data.pin.toByteArray())
                .map {
                    if (it) {
                        State.UNLOCKED
                    } else {
                        throw IllegalArgumentException()
                    }
                }.toObservable()
                .onErrorReturnItem(State.CHECK_PIN_NOTIFICATION)
                .startWith(State.LOADING)
    }

    private fun setupPin(data: SecurityContract.SetupPin): Observable<State> {
        return encryptionManager.setup(data.pin.toByteArray())
                .map {
                    if (it) {
                        State.UNLOCKED
                    } else {
                        throw IllegalArgumentException()
                    }
                }.toObservable()
                .onErrorReturnItem(State.NOT_SAME_PINS_NOTIFICATION)
                .startWith(State.LOADING)
    }

    enum class State {
        LOADING,
        FATAL_ERROR,

        UNINITIALIZED,
        LOCKED,
        UNLOCKED,

        CHECK_PIN_NOTIFICATION,
        NOT_SAME_PINS_NOTIFICATION,
    }
}