package pm.gnosis.heimdall.ui.safe.create

import android.arch.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.model.Solidity

abstract class SafeRecoveryPhraseContract : ViewModel() {
    abstract fun setup(chromeExtensionAddress: Solidity.Address)
    abstract fun generateRecoveryPhrase(): Single<String>
    abstract fun loadEncryptedRecoveryPhrase(): Single<String>
    abstract fun getChromeExtensionAddress(): Solidity.Address
}
