package pm.gnosis.heimdall.ui.safe.create

import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.ui.recoveryphrase.ConfirmRecoveryPhraseViewModel
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.model.Solidity

abstract class CreateSafeConfirmRecoveryPhraseContract : ConfirmRecoveryPhraseViewModel() {
    abstract fun setup(authenticatorInfo: AuthenticatorInfo?)
    abstract fun loadOwnerData(): Single<Pair<AuthenticatorInfo?, List<Solidity.Address>>>
}
