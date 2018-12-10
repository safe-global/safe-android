package pm.gnosis.heimdall.ui.settings.general

import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.svalinn.security.EncryptionManager
import javax.inject.Inject

class GeneralSettingsViewModel @Inject constructor(
    private val encryptionManager: EncryptionManager,
    private val tokenRepository: TokenRepository
) : GeneralSettingsContract() {
    override fun isFingerprintAvailable() = encryptionManager.canSetupFingerprint()

    override fun clearFingerprintData(): Single<Result<Unit>> =
        encryptionManager.clearFingerprintData()
            .andThen(Single.just(Unit))
            .mapToResult()

    override fun loadPaymentToken() =
            tokenRepository.loadPaymentToken()
}
