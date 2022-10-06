package io.gnosis.safe.ui.settings.view

import android.content.Context
import android.content.res.TypedArray
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.data.models.Chain
import io.gnosis.data.models.Owner
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewAddressItemBinding
import io.gnosis.safe.utils.imageRes16dp
import io.gnosis.safe.utils.BlockExplorer
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.copyToClipboard
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber

class AddressItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewAddressItemBinding.inflate(LayoutInflater.from(context), this) }

    init {
        readAttributesAndSetupFields(context, attrs)
    }

    var address: Solidity.Address? = null
        private set

    fun setAddress(chain: Chain?, value: Solidity.Address?, ownerType: Owner.Type? = null) {
        with(binding) {
            blockies.setAddress(value)
            address.text = value?.formatOwnerAddress()
            chain?.let {
                link.setOnClickListener {
                    BlockExplorer.forChain(chain)?.showAddress(context, value)
                }
            } ?: link.visible(false, View.INVISIBLE)
            address.setOnClickListener {
                context.copyToClipboard(context.getString(R.string.address_copied), address.text.toString()) {
                    snackbar(view = root, textId = R.string.copied_success)
                }
            }
            ownerType?.let {
                keyType.setImageResource(ownerType.imageRes16dp())
            } ?: hideKeyTypeOverlay()
        }
        address = value

    }
    private fun hideKeyTypeOverlay() {
        binding.keyType.visible(false)
        binding.keyTypeBackground.visible(false)
    }
    var showSeparator: Boolean = false
        set(value) {
            binding.addressDivider.visible(value)
            field = value
        }

    private fun Solidity.Address.formatOwnerAddress(prefixLength: Int = 6, suffixLength: Int = 4): Spannable =
        SpannableStringBuilder(this.asEthereumAddressChecksumString()).apply {
            setSpan(
                ForegroundColorSpan(context.getColorCompat(R.color.label_primary)),
                0,
                prefixLength,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                ForegroundColorSpan(context.getColorCompat(R.color.label_primary)),
                length - suffixLength,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

    private fun readAttributesAndSetupFields(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AddressItem,
            0, 0
        ).also {
            runCatching {
                applyAttributes(context, it)
            }
                .onFailure { Timber.e(it) }
            it.recycle()
        }
    }

    private fun applyAttributes(context: Context, a: TypedArray) {
        binding.addressDivider.visible(a.getBoolean(R.styleable.AddressItem_show_address_separator, false))
    }
}
