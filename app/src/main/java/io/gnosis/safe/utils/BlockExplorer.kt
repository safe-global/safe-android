package io.gnosis.safe.utils

import android.content.Context
import io.gnosis.data.models.Chain
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.openUrl

class BlockExplorer private constructor(
    private val addressUriTemplate: String,
    private val txHashUriTemplate: String
) {

    fun showAddress(context: Context, address: Solidity.Address?) {
        //TODO: uncomment after backend is returnng blockExplorerTemplate in chains response
        //context.openUrl(addressUriTemplate.replace("{{address}}", address?.asEthereumAddressChecksumString() ?: ""))
        context.openUrl("${addressUriTemplate}address/${address?.asEthereumAddressChecksumString()}")
    }

    fun showTransaction(context: Context, txHash: String?) {
        //TODO: uncomment after backend is returnng blockExplorerTemplate in chains response
        //context.openUrl(txHashUriTemplate.replace("{{txHash}}", txHash ?: ""))
        context.openUrl("${txHashUriTemplate}tx/${txHash}")
    }

    companion object {
        fun forChain(chain: Chain?): BlockExplorer? {
            return chain?.let {
                BlockExplorer(it.blockExplorerTemplateAddress, it.blockExplorerTemplateTxHash)
            }
        }
    }
}
