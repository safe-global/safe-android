package pm.gnosis.heimdall.ui.dialogs.ens

import androidx.lifecycle.ViewModel
import io.reactivex.ObservableTransformer
import io.reactivex.schedulers.Schedulers
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.ethereum.EthCall
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.data.repositories.EnsRepository
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.toHexString
import javax.inject.Inject

abstract class EnsInputContract : ViewModel() {
    abstract fun processEnsInput(): ObservableTransformer<CharSequence, Result<Solidity.Address>>

}

class EnsInputViewModel @Inject constructor(
    private val ensRepository: EnsRepository
) : EnsInputContract() {
    override fun processEnsInput(): ObservableTransformer<CharSequence, Result<Solidity.Address>> = ObservableTransformer {
        it.switchMapSingle { input -> ensRepository.resolve(input.toString()).mapToResult() }
    }
}
