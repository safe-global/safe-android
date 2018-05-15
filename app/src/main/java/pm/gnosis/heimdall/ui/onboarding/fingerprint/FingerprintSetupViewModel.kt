package pm.gnosis.heimdall.ui.onboarding.fingerprint

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.svalinn.security.EncryptionManager
import javax.inject.Inject

class FingerprintSetupViewModel @Inject constructor(
    private val encryptionManager: EncryptionManager
) : FingerprintSetupContract() {
    override fun observeFingerprintForSetup(): Observable<Boolean> =
        encryptionManager.observeFingerprintForSetup()
            .subscribeOn(Schedulers.computation())
}
