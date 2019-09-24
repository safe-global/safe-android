package pm.gnosis.heimdall.ui.safe.create

import io.reactivex.Single
import pm.gnosis.heimdall.ui.recoveryphrase.ConfirmRecoveryPhraseViewModel
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity

abstract class CreateSafeConfirmRecoveryPhraseContract : ConfirmRecoveryPhraseViewModel() {
    abstract fun setup(authenticatorInfo: AuthenticatorSetupInfo?)
    abstract fun loadOwnerData(): Single<Pair<AuthenticatorSetupInfo?, List<Solidity.Address>>>
}
