package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewTxLabelBinding

class ActionLabelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewTxLabelBinding.inflate(LayoutInflater.from(context), this) }

    fun setLabel(label: String, @ColorRes color : Int = R.color.label_primary) {
        binding.label.text = label
        binding.label.setTextColor(ContextCompat.getColor(context, color))
    }
}
