package pm.gnosis.android.app.wallet.data.remote

import org.ethereum.geth.Context
import org.ethereum.geth.Node
import pm.gnosis.android.app.wallet.di.module.EthereumModule
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class EthereumConnector @Inject constructor(
        val context: Context,
        @Named(EthereumModule.RINKEBY_NODE) val rinkebyNode: Node) {

    init {
        rinkebyNode.start()
    }

    fun xpto() {
        val suggestedGas = rinkebyNode.ethereumClient.suggestGasPrice(context)
        Timber.d("Suggested gas price: $suggestedGas")
        val sp = rinkebyNode.ethereumClient.syncProgress(Context())
        Timber.d("Current block: " + sp?.currentBlock.toString())
    }
}
