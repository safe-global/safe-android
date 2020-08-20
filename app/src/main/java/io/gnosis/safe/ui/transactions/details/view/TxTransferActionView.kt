package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import io.gnosis.safe.R
import io.gnosis.safe.ui.settings.view.AddressItem
import io.gnosis.safe.utils.dpToPx
import pm.gnosis.model.Solidity

class TxTransferActionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.LEFT
    }

    private fun clear() {
        removeAllViews()
    }

    fun setActionInfo(outgoing: Boolean, amount: String, logoUri: String, address: Solidity.Address) {

        clear()

        if (outgoing) {
            addAmountItem(amount, logoUri)
        } else {
            addAddressItem(address)
        }

        addArrow()

        if (outgoing) {
            addAddressItem(address)
        } else {
            addAmountItem(amount, logoUri)
        }
    }

    private fun addAmountItem(amount: String, logoUri: String) {
        val amountView = AmountView(context)
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(dpToPx(DEFAULT_MARGIN), 0, 0, 0)
        amountView.layoutParams = layoutParams
        amountView.setAmount(amount, logoUri)
        addView(amountView)
    }

    private fun addAddressItem(address: Solidity.Address) {
        val addressItem = AddressItem(context)
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(ITEM_HEIGHT))
        layoutParams.setMargins(0, 0, 0, dpToPx(ADDRESS_BOTTOM_MARGIN))
        addressItem.layoutParams = layoutParams
        addressItem.address = address
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
        private const val ITEM_HEIGHT = 44
        private const val DEFAULT_MARGIN = 16
        private const val ADDRESS_BOTTOM_MARGIN = 8
        private const val ARROW_ICON_PADDING_CORRECTION = 6
    }
}
