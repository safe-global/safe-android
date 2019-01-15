package pm.gnosis.heimdall.ui.safe.pending

import androidx.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.model.Solidity

abstract class DeploySafeProgressContract : ViewModel() {
    abstract fun setup(safeAddress: Solidity.Address?)
    abstract fun notifySafeFunded(): Single<Solidity.Address>
}
