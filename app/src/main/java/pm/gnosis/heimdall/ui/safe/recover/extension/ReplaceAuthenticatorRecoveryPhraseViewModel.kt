package pm.gnosis.heimdall.ui.safe.recover.extension

import io.reactivex.Observable
import pm.gnosis.heimdall.ui.safe.helpers.RecoverSafeOwnersHelper
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity
import javax.inject.Inject

class ReplaceAuthenticatorRecoveryPhraseViewModel @Inject constructor(
    private val recoverSafeOwnersHelper: RecoverSafeOwnersHelper
) : ReplaceAuthenticatorRecoveryPhraseContract() {
    override fun process(input: Input, safeAddress: Solidity.Address, authenticatorInfo: AuthenticatorSetupInfo?): Observable<ViewUpdate> =
        authenticatorInfo?.let {
            recoverSafeOwnersHelper.process(input, safeAddress, authenticatorInfo.authenticator.address, authenticatorInfo.safeOwner)
        } ?: Observable.just<ViewUpdate>(ViewUpdate.RecoverDataError(IllegalStateException("Authenticator is required!")))
}
