package io.gnosis.safe.ui.safe.settings.view

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewAddressItemBinding
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.openUrl
import pm.gnosis.utils.asEthereumAddressString

class AddressItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewAddressItemBinding.inflate(LayoutInflater.from(context), this) }

    var address: Solidity.Address? = null
        set(value) {
            with(binding) {
                blockies.setAddress(value)
                address.text = value?.formatOwnerAddress()
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

    private fun Solidity.Address.formatOwnerAddress(prefixLength: Int = 4, suffixLength: Int = 4): Spannable =
        SpannableStringBuilder(this.asEthereumAddressString()).apply {
            setSpan(
                ForegroundColorSpan(context.getColorCompat(R.color.blue)),
                0,
                prefixLength,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                ForegroundColorSpan(context.getColorCompat(R.color.blue)),
                length - suffixLength,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
}
