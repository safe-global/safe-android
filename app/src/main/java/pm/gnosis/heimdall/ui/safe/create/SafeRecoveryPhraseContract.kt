package pm.gnosis.heimdall.ui.safe.create

import android.arch.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.model.Solidity

abstract class SafeRecoveryPhraseContract : ViewModel() {
    abstract fun generateMnemonic(): Single<String>
    abstract fun getRecoveryAddress(safeRecoveryPhrase: String): Single<Solidity.Address>
}
