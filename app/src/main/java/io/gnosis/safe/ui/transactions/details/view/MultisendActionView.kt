package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.data.models.Chain
import io.gnosis.safe.databinding.ViewMultisendActionBinding
import io.gnosis.safe.utils.abbreviateEthAddress
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible

class MultisendActionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewMultisendActionBinding.inflate(LayoutInflater.from(context), this) }

    fun setData(chain: Chain, address: Solidity.Address, showChainPrefix: Boolean, method: String, name: String? = null) {
        with(binding) {
            addressImage.setAddress(address)
            addressLabel.text = address.asEthereumAddressChecksumString().abbreviateEthAddress(
                if (showChainPrefix) chain.shortName else null
            )
            methodLabel.text = method
            if (!name.isNullOrBlank()) {
                nameLabel.visible(true)
                nameLabel.text = name
            } else {
                nameLabel.visible(false)
            }
        }
    }

    fun loadKnownAddressLogo(addressUri: String?, address: Solidity.Address?) {
        binding.addressImage.loadKnownAddressLogo(addressUri, address)
    }
}
