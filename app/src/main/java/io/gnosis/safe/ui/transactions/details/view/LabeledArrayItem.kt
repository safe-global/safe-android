package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.Chain
import io.gnosis.data.models.transaction.ParamType
import io.gnosis.data.models.transaction.getParamItemType
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewLabeledArrayArrayItemBinding
import io.gnosis.safe.databinding.ViewLabeledArrayItemBinding
import io.gnosis.safe.ui.settings.view.AddressItem
import io.gnosis.safe.ui.settings.view.NamedAddressItem
import io.gnosis.safe.utils.dpToPx
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

    fun showArray(
        chain: Chain,
        showChainPrefix: Boolean,
        copyChainPrefix: Boolean,
        array: List<Any>?,
        paramType: ParamType,
        paramValue: String,
        addressInfoIndex: Map<String, AddressInfo>? = null
    ) {
        binding.arrayItemValues.removeAllViews()
        val typeValues = if (paramType == ParamType.MIXED) {
            paramValue.replace("[]", "").removeSurrounding("(", ")")
        } else {
            paramValue
        }
        nestingLevel = 1
        if (!array.isNullOrEmpty()) {
            array.forEachIndexed { index, value ->
                if (value is List<*>) {
                    addArrayItem(binding.arrayItemValues, value as List<Any>, paramType, typeValues, chain, showChainPrefix, copyChainPrefix)

                } else {
                    if (paramType == ParamType.MIXED) {
                        val valueType = typeValues.split(",")[index]
                        addValueItem(binding.arrayItemValues, value, getParamItemType(valueType), chain, showChainPrefix, copyChainPrefix, addressInfoIndex?.get(value))
                    } else {
                        addValueItem(binding.arrayItemValues, value, paramType, chain, showChainPrefix, copyChainPrefix, addressInfoIndex?.get(value))
                    }
                }
            }
        } else {
            addEmptyValue(binding.arrayItemValues)
        }
    }

    private fun addArrayItem(
        container: ViewGroup,
        values: List<Any>,
        paramType: ParamType,
        typeValues: String,
        chain: Chain,
        showChainPrefix: Boolean,
        copyChainPrefix: Boolean,
        addressInfoIndex: Map<String, AddressInfo>? = null
    ) {
        val arrayItem = ArrayItem(context)
        val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(0, dpToPx(6), 0, 0)
        arrayItem.layoutParams = layoutParams

        if (values.isEmpty()) {
            addEmptyValue(arrayItem.container)
        } else {
            values.forEachIndexed { index, value ->
                if (value is List<*>) {
                    if (nestingLevel < NESTING_LEVEL_THRESHOLD) {
                        nestingLevel++
                        if (paramType == ParamType.MIXED) {
                            val valueType = typeValues.split(",")[index]
                            addArrayItem(arrayItem.container, value as List<Any>, getParamItemType(valueType), valueType, chain, showChainPrefix, copyChainPrefix)
                        } else {
                            addArrayItem(arrayItem.container, value as List<Any>, paramType, typeValues, chain, showChainPrefix, copyChainPrefix)
                        }
                    } else {
                        addValueItem(arrayItem.container, context.getString(R.string.array), ParamType.VALUE, chain, showChainPrefix, copyChainPrefix)
                    }
                } else {
                    if (paramType == ParamType.MIXED) {
                        val valueType = typeValues.split(",")[index]
                        addValueItem(arrayItem.container, value, getParamItemType(valueType), chain, showChainPrefix, copyChainPrefix, addressInfoIndex?.get(value))
                    } else {
                        addValueItem(arrayItem.container, value, paramType, chain, showChainPrefix, copyChainPrefix, addressInfoIndex?.get(value))
                    }
                }
            }
        }
        nestingLevel--
        container.addView(arrayItem)
    }

    private fun addValueItem(
        container: ViewGroup,
        value: Any,
        paramType: ParamType,
        chain: Chain,
        showChainPrefix: Boolean,
        copyChainPrefix: Boolean,
        addressInfo: AddressInfo? = null
    ) {
        when (paramType) {
            ParamType.ADDRESS -> {
                val addressItem: View
                if (addressInfo != null) {
                    addressItem = NamedAddressItem(context)
                    val address = (value as String).asEthereumAddress()
                    addressItem.setAddress(chain, address, showChainPrefix, copyChainPrefix)
                    addressItem.name = addressInfo.name
                    addressItem.loadKnownAddressLogo(addressInfo.logoUri, address)
                } else {
                    addressItem = AddressItem(context)
                    addressItem.setAddress(chain, (value as String).asEthereumAddress(), showChainPrefix, copyChainPrefix)
                }
                val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                layoutParams.setMargins(dpToPx(-16), dpToPx(8), dpToPx(-16), 0)
                addressItem.layoutParams = layoutParams
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
            ParamType.MIXED -> {

            }
        }
    }

    private fun addEmptyValue(container: ViewGroup) {
        val valueItem = TextView(context, null, 0, R.style.TextMedium)
        val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(0, dpToPx(6), 0, 0)
        valueItem.layoutParams = layoutParams
        valueItem.setTextColor(context.getColorCompat(R.color.label_tertiary))
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
