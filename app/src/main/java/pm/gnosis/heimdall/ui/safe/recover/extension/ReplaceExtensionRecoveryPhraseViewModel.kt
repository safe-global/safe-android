package pm.gnosis.heimdall.ui.safe.recover.extension

import io.reactivex.Observable
import pm.gnosis.heimdall.ui.safe.helpers.RecoverSafeOwnersHelper
import pm.gnosis.model.Solidity
import javax.inject.Inject

class ReplaceExtensionRecoveryPhraseViewModel @Inject constructor(
    private val recoverSafeOwnersHelper: RecoverSafeOwnersHelper
) : ReplaceExtensionRecoveryPhraseContract() {
    override fun process(input: Input, safeAddress: Solidity.Address, extensionAddress: Solidity.Address?): Observable<ViewUpdate> =
        extensionAddress?.let {
            recoverSafeOwnersHelper.process(input, safeAddress, extensionAddress, null)
        } ?: Observable.just<ViewUpdate>(ViewUpdate.RecoverDataError(IllegalStateException("Extension is required!")))
}
