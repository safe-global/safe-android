package pm.gnosis.heimdall.ui.safe.recover.phrase

import io.reactivex.Observable
import pm.gnosis.heimdall.ui.safe.helpers.RecoverSafeOwnersHelper
import pm.gnosis.model.Solidity
import javax.inject.Inject

class ReplaceBrowserExtensionRecoveryPhraseViewModel @Inject constructor(
    private val recoverSafeOwnersHelper: RecoverSafeOwnersHelper
) : ReplaceBrowserExtensionRecoveryPhraseContract() {
    override fun process(input: Input, safeAddress: Solidity.Address, extensionAddress: Solidity.Address): Observable<ViewUpdate> =
        recoverSafeOwnersHelper.process(input, safeAddress, extensionAddress)
}
