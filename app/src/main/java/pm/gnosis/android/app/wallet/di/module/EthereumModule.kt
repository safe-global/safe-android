package pm.gnosis.android.app.wallet.di.module

import dagger.Module
import dagger.Provides
import org.ethereum.geth.*
import pm.gnosis.android.app.wallet.data.remote.RinkebyParams
import pm.gnosis.android.app.wallet.di.ApplicationContext
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Module
class EthereumModule(@ApplicationContext val applicationContext: android.content.Context) {
    companion object {
        const val RINKEBY_NODE = "RinkebyNode"
        const val RINKEBY_NODE_CONFIG = "RinkebyNodeConfig"
        const val RINKEBY_NODE_PATH = "RinkebyNodePath"
    }

    @Provides
    @Singleton
    fun providesEthereumContext() = Context()

    @Provides
    @Singleton
    @Named(RINKEBY_NODE)
    fun providesRinkebyNode(@Named(RINKEBY_NODE_CONFIG) nodeConfig: NodeConfig,
                            @Named(RINKEBY_NODE_PATH) nodePath: String) =
            Geth.newNode(nodePath, nodeConfig)


    @Provides
    @Singleton
    @Named(RINKEBY_NODE_CONFIG)
    fun providesRinkebyNodeConfig() =
            NodeConfig().apply {
                val nodes = Enodes()

                RinkebyParams.BOOT_NODES.forEach {
                    nodes.append(Enode(it))
                }

                bootstrapNodes = nodes
                ethereumGenesis = RinkebyParams.GENESIS_BLOCK
                ethereumNetworkID = RinkebyParams.CHAIN_ID
            }

    @Provides
    @Singleton
    @Named(RINKEBY_NODE_PATH)
    fun providesRinkebyNodePath() = File(applicationContext.filesDir, ".rinkeby_node").absolutePath
}