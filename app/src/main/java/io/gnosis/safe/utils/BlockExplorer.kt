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
        context.openUrl(addressUriTemplate.replace("{{address}}", address?.asEthereumAddressChecksumString() ?: ""))
    }

    fun showTransaction(context: Context, txHash: String?) {
        context.openUrl(txHashUriTemplate.replace("{{txHash}}", txHash ?: ""))
    }

    companion object {
        fun forChain(chain: Chain?): BlockExplorer? {
            return chain?.let {
                BlockExplorer(it.blockExplorerTemplateAddress, it.blockExplorerTemplateTxHash)
            }
        }
    }
}
