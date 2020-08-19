package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.safe.databinding.ViewTxAmountBinding
import io.gnosis.safe.utils.loadTokenLogo

class AmountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewTxAmountBinding.inflate(LayoutInflater.from(context), this) }

    fun setAmount(formattedAmount: String, logoUri: String = "local::ethereum") {
        binding.amountTitle.text = formattedAmount
        binding.logo.loadTokenLogo(logoUri)
    }
}
