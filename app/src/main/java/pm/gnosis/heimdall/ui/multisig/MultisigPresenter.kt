package pm.gnosis.heimdall.ui.multisig

import io.reactivex.Flowable
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.data.db.MultisigWallet
import pm.gnosis.heimdall.data.repositories.MultisigRepository
import javax.inject.Inject

class MultisigPresenter @Inject constructor(
        private val multisigRepository: MultisigRepository
): MultisigContract() {
    override fun observeMultisigWallets(): Flowable<List<MultisigWallet>> {
        return multisigRepository.observeMultisigWallets()
    }

    override fun addMultisigWallet(name: String, address: String) =
            multisigRepository.addMultisigWallet(address, name)

    override fun removeMultisigWallet(multisigWallet: MultisigWallet) =
            multisigRepository.removeMultisigWallet(multisigWallet)

    override fun updateMultisigWalletName(address: String, newName: String) =
            multisigRepository.updateMultisigWalletName(address, newName)
}
