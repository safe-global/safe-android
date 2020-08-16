package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.data.models.TransactionInfo
import io.gnosis.data.models.TransferInfo
import io.gnosis.safe.databinding.ViewTxAmountBinding

class AmountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewTxAmountBinding.inflate(LayoutInflater.from(context), this) }

    fun setAmount(txInfo: TransactionInfo) {

        when (txInfo) {
            is TransactionInfo.Custom -> binding.amountTitle.text = txInfo.value
            is TransactionInfo.Transfer -> binding.amountTitle.text = txInfo.transferInfo.value()
        }
    }

    private fun TransferInfo.value(): String? =
        when (this) {
            is TransferInfo.Erc20Transfer -> this.value
            is TransferInfo.Erc721Transfer -> "1"
            is TransferInfo.EtherTransfer -> this.value
        }
}
