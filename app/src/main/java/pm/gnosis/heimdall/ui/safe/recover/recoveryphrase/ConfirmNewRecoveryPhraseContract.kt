package pm.gnosis.heimdall.ui.safe.recover.recoveryphrase

import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.recoveryphrase.ConfirmRecoveryPhraseViewModel
import pm.gnosis.model.Solidity

abstract class ConfirmNewRecoveryPhraseContract : ConfirmRecoveryPhraseViewModel() {
    abstract fun setup(safeAddress: Solidity.Address, authenticatorAddress: Solidity.Address?)
    abstract fun loadTransaction(): Single<Pair<Solidity.Address, SafeTransaction>>
    abstract fun getSafeAddress(): Solidity.Address
}
