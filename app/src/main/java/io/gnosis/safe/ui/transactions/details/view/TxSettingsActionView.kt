package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.annotation.StringRes
import io.gnosis.safe.R
import io.gnosis.safe.ui.settings.view.AddressItem
import io.gnosis.safe.ui.settings.view.NamedAddressItem
import io.gnosis.safe.utils.dpToPx
import pm.gnosis.model.Solidity

class TxSettingsActionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    init {
        orientation = VERTICAL
        gravity = Gravity.LEFT
    }

    private fun clear() {
        removeAllViews()
    }

    fun setActionInfoItems(actionInfoItems: List<ActionInfoItem>) {
        clear()
        actionInfoItems.forEach { actionInfoItem ->
            addStringItem(context.getString(actionInfoItem.itemLabel!!))

            when (actionInfoItem) {
                is ActionInfoItem.Value -> {
                    addStringItem(actionInfoItem.value, R.color.dark_grey, DEFAULT_MARGIN)
                }
                is ActionInfoItem.Address -> {
                    addAddressItem(actionInfoItem.address)
                }
                is ActionInfoItem.AddressWithLabel -> {
                    val addressLabel =
                        actionInfoItem.addressLabel?.let {
                            actionInfoItem.addressLabel
                        } ?: io.gnosis.data.R.string.empty_string
                    addNamedAddressItem(actionInfoItem.address, addressLabel)
                }
            }
        }
    }


    private fun addStringItem(text: String, color: Int = R.color.gnosis_dark_blue, marginBottom: Int = 0) {
        val actionLabel = ActionLabelView(context)
        val layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(0, 0, 0, dpToPx(marginBottom))
        actionLabel.layoutParams = layoutParams
        actionLabel.setLabel(text, color)
        addView(actionLabel)
    }

    private fun addAddressItem(address: Solidity.Address?) {
        val addressItem = AddressItem(context)
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, resources.getDimension(R.dimen.item_address).toInt())
        addressItem.layoutParams = layoutParams
        addressItem.address = address
        addView(addressItem)
    }

    private fun addNamedAddressItem(address: Solidity.Address?, @StringRes label: Int) {
        val addressItem = NamedAddressItem(context)
        addressItem.address = address
        addressItem.name = label
        addressItem.showSeparator = false
        addView(addressItem)
    }

    companion object {
        private const val DEFAULT_MARGIN = 16
    }
}

sealed class ActionInfoItem {
    abstract val itemLabel: Int?

    data class Value(
        @StringRes override val itemLabel: Int?,
        val value: String
    ) : ActionInfoItem()

    data class Address(
        @StringRes override val itemLabel: Int?,
        val address: Solidity.Address?
    ) : ActionInfoItem()

    data class AddressWithLabel(
        @StringRes override val itemLabel: Int?,
        val address: Solidity.Address?,
        @StringRes val addressLabel: Int? = null,
        @StringRes val addressLabelRes: Int = 0
    ) : ActionInfoItem()
}
