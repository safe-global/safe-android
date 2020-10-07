package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewTxDataBinding
import pm.gnosis.svalinn.common.utils.copyToClipboard
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible

class TxDataView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewTxDataBinding.inflate(LayoutInflater.from(context), this) }

    private var collapsed: Boolean = true

    fun setData(txData: String?, size: Int, title: String? = null) {
        with(binding) {
            if (!title.isNullOrBlank()) {
                dataTitle.text = title
            } else {
                dataTitle.visible(false)
            }
            dataSize.text = "$size bytes"
            if (txData != null) {
                data.text = txData
                collapseChevron.visible(true)

                data.setOnClickListener {
                    context.copyToClipboard(context.getString(R.string.data_copied), data.text.toString()) {
                        snackbar(view = root, textId = R.string.copied_success)
                    }
                }

                root.setOnClickListener {
                    if (collapsed) {
                        expand()
                    } else {
                        collapse()
                    }
                }
            } else {
                collapseChevron.visible(false)
            }

            collapseChevron.setImageResource(R.drawable.ic_chevron_down)
            data.visible(false)
        }
    }

    private fun collapse() {
        collapsed = true
        with(binding) {
            collapseChevron.setImageResource(R.drawable.ic_chevron_down)
            data.visible(false)
        }
    }

    private fun expand() {
        collapsed = false
        with(binding) {
            collapseChevron.setImageResource(R.drawable.ic_chevron_up)
            data.visible(true)
        }
    }
}
