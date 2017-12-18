package pm.gnosis.heimdall.ui.security.unlock

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.security.FingerprintUnlockResult

abstract class UnlockContract : ViewModel() {
    abstract fun checkState(): Observable<Result<State>>
    abstract fun unlock(password: String): Observable<Result<State>>
    abstract fun watchFingerprint(): Observable<Result<FingerprintUnlockResult>>

    enum class State {
        UNINITIALIZED,
        LOCKED,
        UNLOCKED,
    }
}
