package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import io.gnosis.data.models.Chain
import io.gnosis.safe.R
import io.gnosis.safe.ui.settings.view.AddressItem
import io.gnosis.safe.ui.settings.view.NamedAddressItem
import io.gnosis.safe.utils.dpToPx
import pm.gnosis.model.Solidity

class TxTransferActionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = VERTICAL
        gravity = Gravity.LEFT
    }

    private fun clear() {
        removeAllViews()
    }

    fun setActionInfo(
        chain: Chain,
        outgoing: Boolean,
        amount: String,
        logoUri: String,
        address: Solidity.Address,
        showChainPrefix: Boolean,
        copyChainPrefix: Boolean,
        tokenId: String = "",
        addressUri: String? = null,
        addressName: String? = null
    ) {

        clear()

        if (outgoing) {
            if (tokenId.isNotEmpty()) {
                addErc721Item(logoUri, outgoing, tokenId, amount)
            } else {
                addAmountItem(amount, logoUri, outgoing)
            }
        } else {
            if (addressName != null) {
                addNamedAddressItem(chain, address, showChainPrefix, copyChainPrefix, addressName, addressUri)
            } else {
                addAddressItem(chain, address, showChainPrefix, copyChainPrefix)
            }
        }

        addArrow()

        if (outgoing) {
            if (addressName != null) {
                addNamedAddressItem(chain, address, showChainPrefix, copyChainPrefix, addressName, addressUri)
            } else {
                addAddressItem(chain, address, showChainPrefix, copyChainPrefix)
            }
        } else {
            if (tokenId.isNotEmpty()) {
                addErc721Item(logoUri, outgoing, tokenId, amount)
            } else {
                addAmountItem(amount, logoUri, outgoing)
            }
        }
    }

    private fun addErc721Item(logoUri: String, outgoing: Boolean, tokenId: String?, amount: String?) {
        val tokenView = Erc721View(context)
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(dpToPx(DEFAULT_MARGIN), 0, 0, 0)
        tokenView.layoutParams = layoutParams
        tokenView.setToken(logoUri, tokenId, outgoing, amount)
        addView(tokenView)
    }

    private fun addAmountItem(amount: String, logoUri: String, outgoing: Boolean) {
        val amountView = AmountView(context)
        val color = if (outgoing) R.color.label_primary else R.color.primary
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(dpToPx(DEFAULT_MARGIN), 0, 0, 0)
        amountView.layoutParams = layoutParams
        amountView.setAmount(amount, logoUri, color)
        addView(amountView)
    }

    private fun addAddressItem(chain: Chain, address: Solidity.Address, showChainPrefix: Boolean, copyChainPrefix: Boolean) {
        val addressItem = AddressItem(context)
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(ITEM_HEIGHT))
        layoutParams.setMargins(0, 0, 0, 0)
        addressItem.layoutParams = layoutParams
        addressItem.setAddress(chain, address, showChainPrefix, copyChainPrefix)
        addView(addressItem)
    }

    private fun addNamedAddressItem(
        chain: Chain,
        address: Solidity.Address,
        showChainPrefix: Boolean,
        copyChainPrefix: Boolean,
        name: String,
        addressUri: String?
    ) {
        val addressItem = NamedAddressItem(context)
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(ITEM_HEIGHT))
        layoutParams.setMargins(0, 0, 0, 0)
        addressItem.layoutParams = layoutParams
        addressItem.setAddress(chain, address, showChainPrefix, copyChainPrefix)
        addressItem.name = name
        addressItem.loadKnownAddressLogo(addressUri, address)
        addView(addressItem)
    }

    private fun addArrow() {
        val arrowDownView = ImageView(context)
        val arrowDown = ContextCompat.getDrawable(context, R.drawable.ic_arrow_down)
        val layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(dpToPx(DEFAULT_MARGIN), 0, 0, 0)
        arrowDownView.setPadding(dpToPx(ARROW_ICON_PADDING_CORRECTION), 0, dpToPx(ARROW_ICON_PADDING_CORRECTION), 0)
        arrowDownView.setImageDrawable(arrowDown)
        arrowDownView.layoutParams = layoutParams
        addView(arrowDownView)
    }

    companion object {
        private const val ITEM_HEIGHT = 68
        private const val DEFAULT_MARGIN = 16
        private const val ARROW_ICON_PADDING_CORRECTION = 6
    }
}
