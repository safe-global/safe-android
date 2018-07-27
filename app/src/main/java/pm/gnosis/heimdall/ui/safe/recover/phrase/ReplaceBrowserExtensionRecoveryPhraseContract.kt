package pm.gnosis.heimdall.ui.safe.recover.phrase

import io.reactivex.Single
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseContract
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature

abstract class ReplaceBrowserExtensionRecoveryPhraseContract : InputRecoveryPhraseContract() {
    abstract fun recoverAddresses(txHash: ByteArray, signatures: Collection<Signature>): Single<List<Pair<Solidity.Address, Signature>>>
}
