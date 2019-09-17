package pm.gnosis.heimdall.ui.safe.create

import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.ui.recoveryphrase.ConfirmRecoveryPhraseViewModel
import pm.gnosis.model.Solidity

abstract class CreateSafeConfirmRecoveryPhraseContract : ConfirmRecoveryPhraseViewModel() {
    abstract fun setup(authenticatorAddress: Solidity.Address?, safeOwner: AccountsRepository.SafeOwner?)
    abstract fun loadOwnerData(): Single<Pair<AccountsRepository.SafeOwner?, List<Solidity.Address>>>
}
