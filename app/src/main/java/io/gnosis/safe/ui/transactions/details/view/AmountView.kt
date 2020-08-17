package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.data.backend.dto.TransactionDirection
import io.gnosis.data.models.TransactionInfo
import io.gnosis.data.models.TransferInfo
import io.gnosis.safe.databinding.ViewTxAmountBinding
import io.gnosis.safe.utils.formatAmount
import io.gnosis.safe.utils.loadTokenLogo
import java.math.BigInteger

class AmountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewTxAmountBinding.inflate(LayoutInflater.from(context), this) }

    fun setAmount(txInfo: TransactionInfo) {
        when (txInfo) {
            is TransactionInfo.Custom -> binding.amountTitle.text = txInfo.value.formatAmount(true, 18, "ETH")
            is TransactionInfo.Transfer -> {
                val incoming = txInfo.direction == TransactionDirection.INCOMING
                val decimals = when (txInfo.transferInfo) {
                    is TransferInfo.Erc20Transfer -> {
                        (txInfo.transferInfo as TransferInfo.Erc20Transfer).decimals
                    }
                    is TransferInfo.EtherTransfer -> 18
                    else -> 0
                }
                val symbol: String = when (val transferInfo = txInfo.transferInfo) {
                    is TransferInfo.Erc20Transfer -> {
                        transferInfo.tokenSymbol ?: ""
                    }
                    is TransferInfo.Erc721Transfer -> {
                        transferInfo.tokenSymbol ?: ""
                    }
                    else -> {
                        "ETH"
                    }
                }
                binding.amountTitle.text = txInfo.transferInfo.value()?.formatAmount(incoming, decimals!!, symbol)

                val logoUri = when (val transferInfo = txInfo.transferInfo) {
                    is TransferInfo.Erc20Transfer -> {
                        transferInfo.logoUri
                    }
                    is TransferInfo.Erc721Transfer -> {
                        transferInfo.logoUri
                    }
                    else -> {
                        "local::ethereum"
                    }
                }
                binding.logo.loadTokenLogo(logoUri)
            }
        }
    }

    private fun TransferInfo.value(): BigInteger? =
        when (this) {
            is TransferInfo.Erc20Transfer -> this.value
            is TransferInfo.Erc721Transfer -> BigInteger.ONE
            is TransferInfo.EtherTransfer -> this.value
        }
}
