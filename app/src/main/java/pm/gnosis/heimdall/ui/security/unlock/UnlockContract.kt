package pm.gnosis.heimdall.ui.security.unlock

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.security.FingerprintUnlockResult

abstract class UnlockContract : ViewModel() {
    abstract fun checkState(forceConfirmCredentials: Boolean): Observable<Result<State>>
    abstract fun unlock(password: String): Observable<Result<State>>
    abstract fun observeFingerprint(): Observable<Result<FingerprintUnlockResult>>

    enum class State {
        UNINITIALIZED,
        LOCKED,
        UNLOCKED,
    }
}
