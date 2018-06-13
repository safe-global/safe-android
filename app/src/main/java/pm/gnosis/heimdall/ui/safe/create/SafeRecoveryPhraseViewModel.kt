package pm.gnosis.heimdall.ui.safe.create

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.R
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.security.EncryptionManager
import javax.inject.Inject

class SafeRecoveryPhraseViewModel @Inject constructor(
    private val bip39: Bip39,
    private val encryptionManager: EncryptionManager
) : SafeRecoveryPhraseContract() {
    private var mnemonic: String? = null
    private lateinit var chromeExtensionAddress: Solidity.Address

    override fun setup(chromeExtensionAddress: Solidity.Address) {
        this.chromeExtensionAddress = chromeExtensionAddress
    }

    override fun generateRecoveryPhrase(): Single<String> =
        Single.fromCallable { bip39.generateMnemonic(languageId = R.id.english) }
            .doOnSuccess { this.mnemonic = it }
            .subscribeOn(Schedulers.io())

    override fun loadEncryptedRecoveryPhrase(): Single<String> = Single.fromCallable {
        if (mnemonic == null) throw IllegalStateException("Recovery phrase is null")
        encryptionManager.encrypt(mnemonic!!.toByteArray()).toString()
    }.subscribeOn(Schedulers.io())

    override fun getChromeExtensionAddress() = chromeExtensionAddress
}
