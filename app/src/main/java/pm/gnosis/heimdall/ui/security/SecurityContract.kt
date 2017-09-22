package pm.gnosis.heimdall.ui.security

import android.os.SystemClock
import pm.gnosis.heimdall.ui.base.BaseContract


object SecurityContract {

    enum class State {
        UNKNOWN,
        UNINITIALIZED,
        LOCKED,
        UNLOCKED,
        ERROR
    }

    data class SetupPin(val pin: String, val repeat: String) : BaseContract.UiEvent

    data class Unlock(val pin: String) : BaseContract.UiEvent

    data class ViewState(val securityState: State, val loading: Boolean, val notification: Notification? = null) {
        companion object {
            fun initial() = ViewState(State.UNKNOWN, true)
            fun locked() = ViewState(State.LOCKED, false)
            fun unlocked() = ViewState(State.UNLOCKED, false)
            fun uninitialized() = ViewState(State.UNINITIALIZED, false)
            fun error() = ViewState(State.ERROR, false)
        }
    }

    data class Notification(val message: String, private val maxDelay: Long = 500, private val displayAt: Long = SystemClock.elapsedRealtime()) {
        fun shouldDisplay() = displayAt + maxDelay >= SystemClock.elapsedRealtime()
    }

    class ViewModelHolder(vm: ViewModel) : BaseContract.ViewModelHolder<BaseContract.UiEvent, ViewState, ViewModel>(vm)

    interface ViewModel : BaseContract.BaseViewModel<BaseContract.UiEvent, ViewState>
}