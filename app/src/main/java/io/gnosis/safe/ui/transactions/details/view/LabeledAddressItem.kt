package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.data.models.Chain
import io.gnosis.safe.databinding.ViewLabeledAddressItemBinding
import pm.gnosis.model.Solidity


class LabeledAddressItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewLabeledAddressItemBinding.inflate(LayoutInflater.from(context), this)

    var label: CharSequence? = null
        set(value) {
            binding.label.text = value
            field = value
        }

    var address: Solidity.Address? = null
        private set

    fun setAddress(
        chain: Chain,
        value: Solidity.Address,
        showChainPrefix: Boolean,
        copyChainPrefix: Boolean
    ) {
        binding.value.setAddress(chain, value, showChainPrefix, copyChainPrefix)
        address = value
    }
}
