package pm.gnosis.heimdall.ui.security

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import pm.gnosis.heimdall.common.util.Result


abstract class SecurityContract : ViewModel() {
    abstract fun checkState(): Observable<Result<State>>

    abstract fun setupPin(pin: String, repeat: String): Observable<Result<State>>

    abstract fun unlockPin(pin: String): Observable<Result<State>>

    enum class State {
        UNKNOWN,
        UNINITIALIZED,
        LOCKED,
        UNLOCKED,
    }
}