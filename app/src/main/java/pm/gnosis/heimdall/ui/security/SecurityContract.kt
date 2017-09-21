package pm.gnosis.heimdall.ui.security

import io.reactivex.Observable


object SecurityContract {

    enum class State {
        UNKNOWN,
        UNINITIALIZED,
        LOCKED,
        UNLOCKED,
        ERROR
    }

    data class ViewState(val securityState: State, val loading: Boolean) {
        companion object {
            fun initial() = ViewState(State.UNKNOWN, true)
            fun locked() = ViewState(State.LOCKED, false)
            fun unlocked() = ViewState(State.UNLOCKED, false)
            fun uninitialized() = ViewState(State.UNINITIALIZED, false)
            fun error() = ViewState(State.ERROR, false)
        }
    }

    interface Presenter {
        fun start()
        fun stop()

        fun observeViewState(): Observable<ViewState>
        fun observeNotifications(): Observable<String>

        fun unlockPin(pin: String)
        fun setupPin(pin: String, repeat: String)
    }
}