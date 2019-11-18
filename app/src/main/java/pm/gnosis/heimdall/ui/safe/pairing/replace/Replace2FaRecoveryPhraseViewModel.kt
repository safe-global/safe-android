package pm.gnosis.heimdall.ui.safe.pairing.replace

import io.reactivex.Observable
import pm.gnosis.heimdall.ui.safe.helpers.RecoverSafeOwnersHelper
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity
import javax.inject.Inject

class Replace2FaRecoveryPhraseViewModel @Inject constructor(
    private val recoverSafeOwnersHelper: RecoverSafeOwnersHelper
) : Replace2FaRecoveryPhraseContract() {
    override fun process(input: Input, safeAddress: Solidity.Address, authenticatorInfo: AuthenticatorSetupInfo?): Observable<ViewUpdate> =
        authenticatorInfo?.let {
            recoverSafeOwnersHelper.process(input, safeAddress, authenticatorInfo.authenticator.address, authenticatorInfo.safeOwner)
        } ?: Observable.just<ViewUpdate>(ViewUpdate.RecoverDataError(IllegalStateException("Authenticator is required!")))
}
