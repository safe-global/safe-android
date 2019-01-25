package pm.gnosis.heimdall.ui.safe.pairing

import androidx.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.model.Solidity

abstract class PairingContract : ViewModel() {
    abstract fun pair(payload: String): Single<Solidity.Address>
}
