package pm.gnosis.heimdall.ui.settings.general

import io.reactivex.Single
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.svalinn.security.EncryptionManager
import javax.inject.Inject

class GeneralSettingsViewModel @Inject constructor(
    private val encryptionManager: EncryptionManager,
    private val safeRepository: GnosisSafeRepository
) : GeneralSettingsContract() {
    override fun isFingerprintAvailable() = encryptionManager.canSetupFingerprint()

    override fun clearFingerprintData(): Single<Result<Unit>> =
        encryptionManager.clearFingerprintData()
            .andThen(Single.just(Unit))
            .mapToResult()

    override fun loadSafeAdresses(): Single<List<String>> =
        safeRepository.observeAllSafes()
            .map {
                val addresses = mutableListOf<String>()
                it.forEach { safe ->
                    addresses.add(safe.address().asEthereumAddressChecksumString())
                }
                addresses as List<String>
            }
            .firstOrError()
}
