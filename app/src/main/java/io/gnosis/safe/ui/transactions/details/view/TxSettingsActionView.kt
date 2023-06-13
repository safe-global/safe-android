package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.annotation.StringRes
import io.gnosis.data.models.Chain
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

    fun setActionInfoItems(chain: Chain, showChainPrefix: Boolean, copyChainPrefix: Boolean, actionInfoItems: List<ActionInfoItem>) {
        clear()
        actionInfoItems.forEach { actionInfoItem ->
            addStringItem(context.getString(actionInfoItem.itemLabel!!))

            when (actionInfoItem) {
                is ActionInfoItem.Value -> {
                    addStringItem(actionInfoItem.value, R.color.label_secondary, DEFAULT_MARGIN)
                }
                is ActionInfoItem.Address -> {
                    addAddressItem(chain, actionInfoItem.address, showChainPrefix, copyChainPrefix)
                }
                is ActionInfoItem.AddressWithLabel -> {
                    addNamedAddressItem(
                        chain = chain,
                        address = actionInfoItem.address,
                        showChainPrefix = showChainPrefix,
                        copyChainPrefix = copyChainPrefix,
                        label = actionInfoItem.addressLabel,
                        addressUrl = actionInfoItem.addressUrl
                    )
                }
            }
        }
    }

    private fun addStringItem(text: String, color: Int = R.color.label_primary, marginBottom: Int = 0) {
        val actionLabel = ActionLabelView(context)
        val layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(0, 0, 0, dpToPx(marginBottom))
        actionLabel.layoutParams = layoutParams
        actionLabel.setLabel(text, color)
        addView(actionLabel)
    }

    private fun addAddressItem(chain: Chain, address: Solidity.Address?, showChainPrefix: Boolean, copyChainPrefix: Boolean) {
        val addressItem = AddressItem(context)
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, resources.getDimension(R.dimen.item_address).toInt())
        addressItem.layoutParams = layoutParams
        addressItem.setAddress(chain, address, showChainPrefix, copyChainPrefix)
        addView(addressItem)
    }

    private fun addNamedAddressItem(
        chain: Chain,
        address: Solidity.Address?,
        showChainPrefix: Boolean,
        copyChainPrefix: Boolean,
        label: String?,
        addressUrl: String?
    ) {
        val addressItem = NamedAddressItem(context)
        addressItem.setAddress(chain, address, showChainPrefix, copyChainPrefix)
        addressItem.name = label
        addressItem.showSeparator = false
        address?.let {
            addressItem.loadKnownAddressLogo(addressUrl, address)
        }
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
        val addressLabel: String? = null,
        val addressUrl: String? = null
    ) : ActionInfoItem()
}
