package io.gnosis.safe.ui.settings.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewLabeledAddressItemBinding
import io.gnosis.safe.utils.abbreviateEthAddress
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.openUrl
import pm.gnosis.svalinn.common.utils.visible

class LabeledAddressItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewLabeledAddressItemBinding.inflate(LayoutInflater.from(context), this) }

    var address: Solidity.Address? = null
        set(value) {
            with(binding) {
                blockies.setAddress(value)
                address.text = value?.asEthereumAddressChecksumString()?.abbreviateEthAddress()
                binding.root.setOnClickListener {
                    context.openUrl(
                        context.getString(
                            R.string.etherscan_address_url,
                            value?.asEthereumAddressChecksumString()
                        )
                    )
                }
            }
            field = value
        }

    var label: String? = null
        set(value) {
            if(value.isNullOrBlank()) {
                binding.label.visible(false)
            } else {
                binding.label.visible(true)
                binding.label.text = value
            }
            field = value
        }
}
