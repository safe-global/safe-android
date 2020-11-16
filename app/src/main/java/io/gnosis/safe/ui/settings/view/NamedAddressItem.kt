package io.gnosis.safe.ui.settings.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewNamedAddressItemBinding
import io.gnosis.safe.utils.abbreviateEthAddress
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.copyToClipboard
import pm.gnosis.svalinn.common.utils.openUrl
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
        set(value) {
            with(binding) {
                blockies.setAddress(value)
                address.text = value?.asEthereumAddressChecksumString()?.abbreviateEthAddress()
                binding.link.setOnClickListener {
                    context.openUrl(
                        context.getString(
                            R.string.etherscan_address_url,
                            value?.asEthereumAddressChecksumString()
                        )
                    )
                }
                binding.root.setOnClickListener {
                    value?.let {
                        context.copyToClipboard(context.getString(R.string.address_copied), value.asEthereumAddressChecksumString()) {
                            snackbar(view = root, textId = R.string.copied_success)
                        }
                    }
                }
            }
            field = value
        }

    var name: Int? = null
        set(value) {
            if (value == null) {
                binding.name.visible(false)
            } else {
                binding.name.visible(true)
                binding.name.setText(value)
            }
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
}
