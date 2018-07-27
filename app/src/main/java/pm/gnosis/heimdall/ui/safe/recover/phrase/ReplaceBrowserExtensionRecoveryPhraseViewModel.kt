package pm.gnosis.heimdall.ui.safe.recover.phrase

import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.ui.safe.helpers.RecoverSafeOwnersHelper
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import javax.inject.Inject

class ReplaceBrowserExtensionRecoveryPhraseViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val recoverSafeOwnersHelper: RecoverSafeOwnersHelper
) : ReplaceBrowserExtensionRecoveryPhraseContract() {
    override fun process(input: Input, safeAddress: Solidity.Address, extensionAddress: Solidity.Address): Observable<ViewUpdate> =
        recoverSafeOwnersHelper.process(input, safeAddress, extensionAddress)

    override fun recoverAddresses(txHash: ByteArray, signatures: Collection<Signature>): Single<List<Pair<Solidity.Address, Signature>>> =
        Single.zip(signatures.map { signature ->
            accountsRepository.recover(txHash, signature).map { it to signature }
        }) { pairs -> (pairs.map { it as Pair<Solidity.Address, Signature> }).toList() }
}
