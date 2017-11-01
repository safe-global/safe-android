package pm.gnosis.heimdall.ui.multisig.overview

import io.reactivex.Flowable
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.scanToAdapterData
import java.math.BigInteger
import javax.inject.Inject

class MultisigOverviewViewModel @Inject constructor(
        private val multisigRepository: GnosisSafeRepository
) : MultisigOverviewContract() {
    override fun observeMultisigWallets(): Flowable<Result<Adapter.Data<Safe>>> {
        return multisigRepository.observeSafes()
                .scanToAdapterData({ (prevAddress), (newAddress) -> prevAddress == newAddress })
                .mapToResult()
    }

    override fun addMultisigWallet(address: BigInteger, name: String) =
            multisigRepository.add(address, name)

    override fun removeMultisigWallet(address: BigInteger) =
            multisigRepository.remove(address)

    override fun updateMultisigWalletName(address: BigInteger, newName: String) =
            multisigRepository.updateName(address, newName)
}
