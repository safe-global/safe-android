package pm.gnosis.android.app.wallet.data.remote

import org.ethereum.geth.*

class EthereumConnector {
    val RINKEBY_NODE: NodeConfig by lazy {
        NodeConfig().apply {
            val nodes = Enodes()

            RinkebyParams.BOOT_NODES.forEach {
                nodes.append(Enode(it))
            }

            bootstrapNodes = nodes
            ethereumGenesis = RinkebyParams.GENESIS_BLOCK
            ethereumNetworkID = RinkebyParams.CHAIN_ID

        }
    }


    fun createEthereumNode(path: String, nodeConfig: NodeConfig = RINKEBY_NODE): Node {
        return Geth.newNode(path, nodeConfig)
    }
}