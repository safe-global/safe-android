package io.gnosis.safe.ui.settings.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.data.models.Chain
import io.gnosis.data.models.Owner
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewNamedAddressItemBinding
import io.gnosis.safe.utils.BlockExplorer
import io.gnosis.safe.utils.abbreviateEthAddress
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.copyToClipboard
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber

class NamedAddressItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewNamedAddressItemBinding.inflate(LayoutInflater.from(context), this) }

    init {
        readAttributesAndSetupFields(context, attrs)
    }

    var address: Solidity.Address? = null
        private set

    fun setAddress(chain: Chain, value: Solidity.Address?, ownerType: Owner.Type? = null) {
        with(binding) {
            blockies.setAddress(value)
            address.text = value?.asEthereumAddressChecksumString()?.abbreviateEthAddress()
            binding.link.setOnClickListener {
                BlockExplorer.forChain(chain)?.showAddress(context, value)
            }
            binding.root.setOnClickListener {
                value?.let {
                    context.copyToClipboard(context.getString(R.string.address_copied), value.asEthereumAddressChecksumString()) {
                        snackbar(view = root, textId = R.string.copied_success)
                    }
                }
            }
        }
        address = value
    }

    var name: String? = null
        set(value) {
            value?.let {
                binding.name.visible(true)
                binding.name.text = it
            } ?: binding.name.visible(false)
            field = value
        }

    private fun readAttributesAndSetupFields(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.NamedAddressItem,
            0, 0
        ).also {
            runCatching {
                applyAttributes(context, it)
            }
                .onFailure { Timber.e(it) }
            it.recycle()
        }
    }

    var showSeparator: Boolean = false
        set(value) {
            binding.namedAddressItemSeparator.visible(value)
            field = value
        }

    private fun applyAttributes(context: Context, a: TypedArray) {
        binding.namedAddressItemSeparator.visible(a.getBoolean(R.styleable.NamedAddressItem_show_named_address_separator, false))
    }

    fun loadKnownAddressLogo(addressUri: String?, address: Solidity.Address?) {
        binding.blockies.loadKnownAddressLogo(addressUri, address)
    }
}
