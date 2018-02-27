package pm.gnosis.heimdall.ui.settings.security

import io.reactivex.Single
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.svalinn.security.EncryptionManager
import javax.inject.Inject

class SecuritySettingsViewModel @Inject constructor(
    private val encryptionManager: EncryptionManager
) : SecuritySettingsContract() {
    override fun isFingerprintAvailable() = encryptionManager.canSetupFingerprint()

    override fun clearFingerprintData(): Single<Result<Unit>> =
        encryptionManager.clearFingerprintData()
            .andThen(Single.just(Unit))
            .mapToResult()
}
