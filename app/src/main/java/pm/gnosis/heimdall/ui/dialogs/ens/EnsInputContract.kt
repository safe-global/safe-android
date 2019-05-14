package pm.gnosis.heimdall.ui.dialogs.ens

import androidx.lifecycle.ViewModel
import io.reactivex.ObservableTransformer
import io.reactivex.schedulers.Schedulers
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.ethereum.EthCall
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.heimdall.BuildConfig
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
    private val ethereumRepository: EthereumRepository
) : EnsInputContract() {
    override fun processEnsInput(): ObservableTransformer<CharSequence, Result<Solidity.Address>> = ObservableTransformer {
        it
            .subscribeOn(Schedulers.computation())
            .switchMap { input ->
                val node = input.split(".").foldRight(ByteArray(32)) { part, node ->
                    Sha3Utils.keccak(node + Sha3Utils.keccak(part.toByteArray()))
                }
                val registerData = GET_RESOLVER + node.toHexString()
                ethereumRepository.request(EthCall(transaction = Transaction(address = ENS_ADDRESS, data = registerData)))
                    .flatMap { resp ->
                        val resolverAddress = resp.result()!!.asEthereumAddress()!!
                        val resolverData = GET_ADDRESS + node.toHexString()
                        ethereumRepository.request(EthCall(transaction = Transaction(address = resolverAddress, data = resolverData)))
                    }
                    .map { resp ->
                        resp.result()!!.asEthereumAddress()!!
                    }
                    .mapToResult()
            }
    }

    companion object {
        private val ENS_ADDRESS = BuildConfig.ENS_REGISTRY.asEthereumAddress()!!
        private val GET_ADDRESS = "0x3b3b57de"
        private val GET_RESOLVER = "0x0178b8bf"
    }
}
