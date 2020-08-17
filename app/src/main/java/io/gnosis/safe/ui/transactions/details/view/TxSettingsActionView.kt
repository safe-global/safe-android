package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import io.gnosis.data.models.TransactionInfo
import io.gnosis.data.repositories.SafeRepository.Companion.DEFAULT_FALLBACK_HANDLER
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_ADD_OWNER_WITH_THRESHOLD
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_CHANGE_MASTER_COPY
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_CHANGE_THRESHOLD
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_DISABLE_MODULE
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_ENABLE_MODULE
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_REMOVE_OWNER
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_SET_FALLBACK_HANDLER
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_SWAP_OWNER
import io.gnosis.data.repositories.getValueByName
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewTxActionBinding
import io.gnosis.safe.ui.settings.view.AddressItem
import io.gnosis.safe.ui.settings.view.LabeledAddressItem
import io.gnosis.safe.ui.transactions.getVersionForAddress
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress

class TxSettingsActionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    init {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.LEFT
    }

    private val binding by lazy { ViewTxActionBinding.inflate(LayoutInflater.from(context), this) }
    private fun clear() {
        removeAllViews()
    }

    fun setActionInfo(txInfo: TransactionInfo) {
        clear()
        val settingsChange = txInfo as TransactionInfo.SettingsChange
        settingsMethodTitle[settingsChange.dataDecoded.method]?.let { addStringItem(it) }

        val params = settingsChange.dataDecoded.parameters
        when (settingsChange.dataDecoded.method) {
            METHOD_CHANGE_MASTER_COPY -> {
                val mainCopy = params.getValueByName("_masterCopy")?.asEthereumAddress()
                val label = mainCopy?.let { it.getVersionForAddress() }
                label?.let { addLabeledAddressItem(mainCopy, it) }
            }
            METHOD_CHANGE_THRESHOLD -> params.getValueByName("_threshold")?.let { addStringItem(it) }
            METHOD_ADD_OWNER_WITH_THRESHOLD -> {
                params.getValueByName("owner")?.asEthereumAddress()?.let { addAddressItem(it) }
                settingsMethodTitle[METHOD_CHANGE_THRESHOLD]?.let { addStringItem(it) }
                params.getValueByName("_threshold")?.let { addStringItem(it, R.color.dark_grey) }
            }

            METHOD_REMOVE_OWNER -> {
                params.getValueByName("owner")?.asEthereumAddress()?.let { addAddressItem(it) }
                settingsMethodTitle[METHOD_CHANGE_THRESHOLD]?.let { addStringItem(it) }
                params.getValueByName("_threshold")?.let { addStringItem(it, R.color.dark_grey) }
            }
            METHOD_SET_FALLBACK_HANDLER -> {
                val fallbackHandler = params.getValueByName("handler")?.asEthereumAddress()
                val label =
                    if (DEFAULT_FALLBACK_HANDLER == fallbackHandler) {
                        R.string.tx_list_default_fallback_handler
                    } else {
                        R.string.tx_list_default_fallback_handler_unknown
                    }
                addLabeledAddressItem(fallbackHandler, context.getString(label))
            }
            METHOD_SWAP_OWNER -> {
                params.getValueByName("oldOwner")?.asEthereumAddress()?.let { addAddressItem(it) }
                settingsMethodTitle[METHOD_ADD_OWNER_WITH_THRESHOLD]?.let { addStringItem(it) }
                params.getValueByName("newOwner")?.asEthereumAddress()?.let { addAddressItem(it) }
            }
            METHOD_ENABLE_MODULE, METHOD_DISABLE_MODULE -> {
                params.getValueByName("module")?.asEthereumAddress()?.let { addAddressItem(it) }
            }
        }
    }

    private fun addStringItem(text: String, color: Int = R.color.gnosis_dark_blue) {
        val actionLabel = ActionLabelView(context)
        actionLabel.setLabel(text, color)
        addView(actionLabel)
    }

    private fun addAddressItem(address: Solidity.Address) {
        val addressItem = AddressItem(context)
        addressItem.address = address
        addView(addressItem)
    }

    private fun addLabeledAddressItem(address: Solidity.Address?, label: String) {
        val addressItem = LabeledAddressItem(context)
        addressItem.address = address
        addressItem.label = label
        addView(addressItem)
    }

    private val settingsMethodTitle = mapOf(
        METHOD_ADD_OWNER_WITH_THRESHOLD to context.getString(R.string.tx_details_add_owner),
        METHOD_CHANGE_MASTER_COPY to context.getString(R.string.tx_details_new_mastercopy),
        METHOD_CHANGE_THRESHOLD to context.getString(R.string.tx_details_change_required_confirmations),
        METHOD_DISABLE_MODULE to context.getString(R.string.tx_details_disable_module),
        METHOD_ENABLE_MODULE to context.getString(R.string.tx_details_enable_module),
        METHOD_REMOVE_OWNER to context.getString(R.string.tx_details_remove_owner),
        METHOD_SET_FALLBACK_HANDLER to context.getString(R.string.tx_details_set_fallback_handler),
        METHOD_SWAP_OWNER to context.getString(R.string.tx_details_remove_owner)
    )
}
