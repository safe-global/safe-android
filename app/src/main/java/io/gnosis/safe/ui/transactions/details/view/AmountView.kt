package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewTxAmountBinding
import io.gnosis.safe.utils.loadTokenLogo
import pm.gnosis.svalinn.common.utils.getColorCompat

class AmountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewTxAmountBinding.inflate(LayoutInflater.from(context), this) }

    fun setAmount(formattedAmount: String, logoUri: String = "local::native_currency", @ColorRes color: Int = R.color.label_primary) {
        binding.amountTitle.text = formattedAmount
        binding.logo.loadTokenLogo(icon = logoUri)
        binding.amountTitle.setTextColor(context.getColorCompat(color))
    }
}
