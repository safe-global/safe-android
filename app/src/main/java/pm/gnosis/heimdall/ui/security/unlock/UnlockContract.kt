package pm.gnosis.heimdall.ui.security.unlock

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import pm.gnosis.heimdall.common.utils.Result

abstract class UnlockContract : ViewModel() {
    abstract fun checkState(): Observable<Result<State>>
    abstract fun unlock(password: String): Observable<Result<State>>

    enum class State {
        UNINITIALIZED,
        LOCKED,
        UNLOCKED,
    }
}
