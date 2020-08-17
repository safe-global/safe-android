package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import io.gnosis.data.backend.dto.TransactionDirection
import io.gnosis.data.models.TransactionInfo
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewTxActionBinding
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

    private val binding by lazy { ViewTxActionBinding.inflate(LayoutInflater.from(context), this) }

    private fun clear() {
        removeAllViews()
    }

    fun setActionInfo(txInfo: TransactionInfo) {

        clear()

        when {
            txInfo is TransactionInfo.Custom -> addAmountItem(txInfo, "")
            txInfo is TransactionInfo.Transfer && txInfo.direction == TransactionDirection.OUTGOING -> addAmountItem(txInfo, "")
            txInfo is TransactionInfo.Transfer && txInfo.direction == TransactionDirection.INCOMING -> addAddressItem(txInfo.sender)
        }

        addArrow()

        when {
            txInfo is TransactionInfo.Transfer && txInfo.direction == TransactionDirection.OUTGOING -> addAddressItem(txInfo.recipient)
            txInfo is TransactionInfo.Custom -> addAddressItem(txInfo.to)
            txInfo is TransactionInfo.Transfer && txInfo.direction == TransactionDirection.INCOMING -> addAmountItem(txInfo, "")
        }
    }

    private fun addAmountItem(formattedAmount: TransactionInfo, icon: String) {
        val amountView = AmountView(context)
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(ITEM_HEIGHT))
        layoutParams.setMargins(dpToPx(DEFAULT_MARGIN), 0, 0, dpToPx(DEFAULT_MARGIN))
        amountView.layoutParams = layoutParams
        amountView.setAmount(formattedAmount)
        addView(amountView)
    }

    private fun addAddressItem(address: Solidity.Address) {
        val addressItem = AddressItem(context)
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(ITEM_HEIGHT))
//        layoutParams.setMargins(dpToPx(DEFAULT_MARGIN), 0, 0, dpToPx(DEFAULT_MARGIN))
        layoutParams.setMargins(0, 0, 0, dpToPx(DEFAULT_MARGIN))
        addressItem.layoutParams = layoutParams
        addressItem.address = address
        addView(addressItem)
    }

    private fun addArrow() {
        val arrowDownView = ImageView(context)
        val arrowDown = ContextCompat.getDrawable(context, R.drawable.ic_arrow_down)
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(24))
        layoutParams.setMargins(dpToPx(DEFAULT_MARGIN + 4), 0, 0, dpToPx(DEFAULT_MARGIN))
        arrowDownView.setImageDrawable(arrowDown)
        arrowDownView.layoutParams = layoutParams
        addView(arrowDownView)
    }

    companion object {
        private const val ITEM_HEIGHT = 44
        private const val DEFAULT_MARGIN = 16
    }
}
