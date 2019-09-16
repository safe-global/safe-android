package pm.gnosis.heimdall.ui.safe.pairing

import androidx.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.model.Solidity

abstract class PairingContract : ViewModel() {
    /**
     * @param signingSafe to indicate which key should be used for pairing. @null if a new key should be generated
     */
    abstract fun pair(payload: String, signingSafe: Solidity.Address?): Single<Pair<AccountsRepository.SafeOwner, Solidity.Address>>
}
