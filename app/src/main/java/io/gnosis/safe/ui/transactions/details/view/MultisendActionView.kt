package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.safe.databinding.ViewMultisendActionBinding
import io.gnosis.safe.utils.abbreviateEthAddress
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity

class MultisendActionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewMultisendActionBinding.inflate(LayoutInflater.from(context), this) }

    fun setData(address: Solidity.Address, method: String, txType: TxType? = null) {
        with(binding) {
            addressImage.setAddress(address)
            addressLabel.text = address.asEthereumAddressChecksumString().abbreviateEthAddress()
            methodLabel.text = method
        }
    }
}
