package io.gnosis.safe.ui.safe.send_funds.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.safe.databinding.ViewTransferAmountBinding
import io.gnosis.safe.utils.loadTokenLogo

class TransferAmountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewTransferAmountBinding.inflate(LayoutInflater.from(context), this) }

    fun setAmount(amountFormatted: String, logoUri: String = "local::native_currency") {
        with(binding) {
            logo.loadTokenLogo(icon = logoUri)
            amount.text = amountFormatted
        }
    }
}
