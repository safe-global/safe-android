package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
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
import io.gnosis.safe.databinding.ViewTxActionBinding
import io.gnosis.safe.ui.settings.view.AddressItem
import io.gnosis.safe.ui.settings.view.LabeledAddressItem
import io.gnosis.safe.ui.transactions.getVersionForAddress
import io.gnosis.safe.utils.dpToPx
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
        addStringItem(settingsMethodTitle.get(settingsChange.dataDecoded.method)!!)

        val params = settingsChange.dataDecoded.parameters
        when (settingsChange.dataDecoded.method) {
            METHOD_CHANGE_MASTER_COPY -> {
                val mainCopy = params.getValueByName("_masterCopy")?.asEthereumAddress()!!
                val label = mainCopy.getVersionForAddress()
                addLabeledAddressItem(mainCopy, label)
            }
            METHOD_CHANGE_THRESHOLD -> addStringItem(params.getValueByName("_threshold")!!)
            METHOD_ADD_OWNER_WITH_THRESHOLD -> {
                addAddressItem(params.getValueByName("owner")?.asEthereumAddress()!!)
                addStringItem(settingsMethodTitle[METHOD_CHANGE_THRESHOLD]!!)
                addStringItem(params.getValueByName("_threshold")!!)
            }

            METHOD_REMOVE_OWNER -> {
                addAddressItem(params.getValueByName("owner")?.asEthereumAddress()!!)
                addStringItem(settingsMethodTitle[METHOD_CHANGE_THRESHOLD]!!)
                addStringItem(params.getValueByName("_threshold")!!)
            }
            METHOD_SET_FALLBACK_HANDLER -> {
                val label =
                    if (DEFAULT_FALLBACK_HANDLER == params.getValueByName("handler")?.asEthereumAddress()) {
                        "DefaultFallBackHandler"
                    } else {
                        "Unknown"
                    }
                addLabeledAddressItem(params.getValueByName("handler")?.asEthereumAddress()!!, label)
            }
            METHOD_SWAP_OWNER -> {
                addAddressItem(params.getValueByName("oldOwner")?.asEthereumAddress()!!)
                addStringItem(settingsMethodTitle[METHOD_ADD_OWNER_WITH_THRESHOLD]!!)
                addAddressItem(params.getValueByName("newOwner")?.asEthereumAddress()!!)
            }
            METHOD_ENABLE_MODULE, METHOD_DISABLE_MODULE -> {
                addAddressItem(params.getValueByName("module")?.asEthereumAddress()!!)
            }
        }
    }

    private fun addStringItem(text: String) {
        val addressItem = TextView(context)
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(TxTransferActionView.ADDRESS_ITEM_HEIGHT))
        layoutParams.setMargins(dpToPx(TxTransferActionView.ADDRESS_ITEM_MARGIN_LEFT), 0, 0, dpToPx(TxTransferActionView.MARGIN_VERTICAL))
        addressItem.layoutParams = layoutParams
        addressItem.text = text
        addView(addressItem)
    }

    private fun addAddressItem(address: Solidity.Address) {
        val addressItem = AddressItem(context)
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(TxTransferActionView.ADDRESS_ITEM_HEIGHT))
        layoutParams.setMargins(dpToPx(TxTransferActionView.ADDRESS_ITEM_MARGIN_LEFT), 0, 0, dpToPx(TxTransferActionView.MARGIN_VERTICAL))
        addressItem.layoutParams = layoutParams
        addressItem.address = address
        addView(addressItem)
    }

    private fun addLabeledAddressItem(address: Solidity.Address, label: String) {
        val addressItem = LabeledAddressItem(context)
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(TxTransferActionView.ADDRESS_ITEM_HEIGHT))
        layoutParams.setMargins(dpToPx(TxTransferActionView.ADDRESS_ITEM_MARGIN_LEFT), 0, 0, dpToPx(TxTransferActionView.MARGIN_VERTICAL))
        addressItem.layoutParams = layoutParams
        addressItem.address = address
        addressItem.label = label
        addView(addressItem)
    }

    private val settingsMethodTitle = mapOf(
        METHOD_ADD_OWNER_WITH_THRESHOLD to "Add owner:",
        METHOD_CHANGE_MASTER_COPY to "New mastercopy:",
        METHOD_CHANGE_THRESHOLD to "Change required confirmations:",
        METHOD_DISABLE_MODULE to "Disable module",
        METHOD_ENABLE_MODULE to "Enable module",
        METHOD_REMOVE_OWNER to "Remove owner: ",
        METHOD_SET_FALLBACK_HANDLER to "Set fallback handler",
        METHOD_SWAP_OWNER to "Remove owner:"

    )
}

