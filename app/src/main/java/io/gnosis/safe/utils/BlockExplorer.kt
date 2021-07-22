package io.gnosis.safe.utils

import android.content.Context
import io.gnosis.data.models.Chain
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.openUrl

class BlockExplorer private constructor(
    private val blockExplorerUrl: String
) {

    fun showAddress(context: Context, address: Solidity.Address?) {
        //FIXME: handle possible differences in address endpoint format for different block explorers
        context.openUrl("${blockExplorerUrl}address/${address?.asEthereumAddressChecksumString()}")
    }

    fun showTransaction(context: Context, txHash: String?) {
        //FIXME: handle possible differences in tx endpoint format for different block explorers
        context.openUrl("${blockExplorerUrl}tx/${txHash}")
    }

    companion object {
        fun forChain(chain: Chain?): BlockExplorer? {
            return chain?.let {
                BlockExplorer(it.blockExplorerUrl)
            }
        }
    }
}
