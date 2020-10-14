package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.data.backend.dto.ParamType
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewLabeledArrayArrayItemBinding
import io.gnosis.safe.databinding.ViewLabeledArrayItemBinding
import io.gnosis.safe.ui.settings.view.AddressItem
import io.gnosis.safe.utils.dpToPx
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.removeHexPrefix


class LabeledArrayItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewLabeledArrayItemBinding.inflate(LayoutInflater.from(context), this)

    private var nestingLevel = 0

    var label: CharSequence? = null
        set(value) {
            binding.arrayItemLabel.text = value
            field = value
        }

    fun showArray(array: List<Any>?, paramType: ParamType) {
        binding.arrayItemValues.removeAllViews()
        nestingLevel = 1
        if (!array.isNullOrEmpty()) {
            array.forEach {
                if (it is List<*>) {
                    addArrayItem(binding.arrayItemValues, it as List<Any>, paramType)

                } else {
                    addValueItem(binding.arrayItemValues, it, paramType)
                }
            }
        } else {
            addEmptyValue(binding.arrayItemValues)
        }
    }

    private fun addArrayItem(container: ViewGroup, values: List<Any>, paramType: ParamType) {
        val arrayItem = ArrayItem(context)
        val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(0, dpToPx(6), 0, 0)
        arrayItem.layoutParams = layoutParams

        if (values.isEmpty()) {
            addEmptyValue(arrayItem.container)
        } else {
            values.forEach {
                if (it is List<*>) {
                    if (nestingLevel < NESTING_LEVEL_THRESHOLD) {
                        nestingLevel++
                        addArrayItem(arrayItem.container, it as List<Any>, paramType)
                    } else {
                        addValueItem(arrayItem.container, context.getString(R.string.array), ParamType.VALUE)
                    }
                } else {
                    addValueItem(arrayItem.container, it, paramType)
                }
            }
        }
        nestingLevel--
        container.addView(arrayItem)
    }

    private fun addValueItem(container: ViewGroup, value: Any, paramType: ParamType) {
        when (paramType) {
            ParamType.ADDRESS -> {
                val addressItem = AddressItem(context)
                val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                layoutParams.setMargins(dpToPx(-16), dpToPx(8), dpToPx(-16), 0)
                addressItem.layoutParams = layoutParams
                addressItem.address = (value as String).asEthereumAddress()
                container.addView(addressItem)
            }
            ParamType.BYTES -> {
                val bytesItem = TxDataView(context)
                val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                layoutParams.setMargins(0, dpToPx(6), 0, 0)
                bytesItem.layoutParams = layoutParams
                bytesItem.setData(value as String, value.removeHexPrefix().length / 2)
                container.addView(bytesItem)
            }
            ParamType.VALUE -> {
                val valueItem = TextView(context, null, 0, R.style.TextMedium)
                val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                layoutParams.setMargins(0, dpToPx(6), 0, 0)
                valueItem.layoutParams = layoutParams
                valueItem.text = value as String
                container.addView(valueItem)
            }
        }
    }

    private fun addEmptyValue(container: ViewGroup) {
        val valueItem = TextView(context, null, 0, R.style.TextMedium)
        val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(0, dpToPx(6), 0, 0)
        valueItem.layoutParams = layoutParams
        valueItem.setTextColor(context.getColorCompat(R.color.medium_grey))
        valueItem.setText(R.string.empty)
        container.addView(valueItem)
    }

    class ArrayItem @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : ConstraintLayout(context, attrs, defStyleAttr) {

        private val binding by lazy { ViewLabeledArrayArrayItemBinding.inflate(LayoutInflater.from(context), this) }

        private var collapsed: Boolean = true

        val container
            get() = binding.arrayItemValues

        init {
            with(binding) {
                arrayItemCollapseChevron.setImageResource(R.drawable.ic_chevron_down)
                arrayItemCollapseChevron.visible(true)
                arrayItemValues.visible(false)
                root.setOnClickListener {
                    if (collapsed) {
                        expand()
                    } else {
                        collapse()
                    }
                }
            }
        }

        private fun collapse() {
            collapsed = true
            with(binding) {
                arrayItemCollapseChevron.setImageResource(R.drawable.ic_chevron_down)
                arrayItemValues.visible(false)
            }
        }

        private fun expand() {
            collapsed = false
            with(binding) {
                arrayItemCollapseChevron.setImageResource(R.drawable.ic_chevron_up)
                arrayItemValues.visible(true)
            }
        }
    }

    companion object {
        private const val NESTING_LEVEL_THRESHOLD = 10
    }
}
