package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewTxDataBinding
import pm.gnosis.svalinn.common.utils.visible

class TxDataView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewTxDataBinding.inflate(LayoutInflater.from(context), this) }

    private var collapsed: Boolean = true

    fun setData(txData: String?, size: Int, title: String? = null) {
        if(!title.isNullOrBlank()) {
            binding.actionTitle.text = title
        }
        binding.dataSize.text = "$size bytes"
        if (txData != null) {
            binding.data.text = txData
            binding.collapseChevron.visible(true)
            binding.root.setOnClickListener {
                if (collapsed) {
                    expand()
                } else {
                    collapse()
                }
            }
        } else {
            binding.collapseChevron.visible(false)
        }

        binding.collapseChevron.setImageResource(R.drawable.ic_chevron_down)
        binding.data.visible(false)
    }

    private fun collapse() {
        collapsed = true
        binding.collapseChevron.setImageResource(R.drawable.ic_chevron_down)
        binding.data.visible(false)
    }

    private fun expand() {
        collapsed = false
        binding.collapseChevron.setImageResource(R.drawable.ic_chevron_up)
        binding.data.visible(true)
    }
}
