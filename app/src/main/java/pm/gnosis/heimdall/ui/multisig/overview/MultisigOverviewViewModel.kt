package pm.gnosis.heimdall.ui.multisig.overview

import io.reactivex.Flowable
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.common.util.mapToResult
import pm.gnosis.heimdall.data.repositories.MultisigRepository
import pm.gnosis.heimdall.data.repositories.model.MultisigWallet
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.scanToAdapterData
import javax.inject.Inject

class MultisigOverviewViewModel @Inject constructor(
        private val multisigRepository: MultisigRepository
): MultisigOverviewContract() {
    override fun observeMultisigWallets(): Flowable<Result<Adapter.Data<MultisigWallet>>> {
        return multisigRepository.observeMultisigWallets()
                .scanToAdapterData({ (prevAddress), (newAddress) -> prevAddress == newAddress })
                .mapToResult()
    }

    override fun addMultisigWallet(name: String, address: String) =
            multisigRepository.addMultisigWallet(address, name)

    override fun removeMultisigWallet(address: String) =
            multisigRepository.removeMultisigWallet(address)

    override fun updateMultisigWalletName(address: String, newName: String) =
            multisigRepository.updateMultisigWalletName(address, newName)
}
