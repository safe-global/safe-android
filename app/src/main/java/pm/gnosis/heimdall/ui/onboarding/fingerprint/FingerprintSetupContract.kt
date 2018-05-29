package pm.gnosis.heimdall.ui.onboarding.fingerprint

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable

abstract class FingerprintSetupContract : ViewModel() {
    abstract fun observeFingerprintForSetup(): Observable<Boolean>
}
