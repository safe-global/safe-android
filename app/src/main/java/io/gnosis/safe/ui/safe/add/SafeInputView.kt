package io.gnosis.safe.ui.safe.add

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import io.gnosis.data.models.Chain
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewSafeInputBinding
import io.gnosis.safe.utils.formatEthAddress
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress

class SafeInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewSafeInputBinding.inflate(LayoutInflater.from(context), this)

    var address: Solidity.Address? = null
        private set

    var hint: String = context.getString(R.string.enter_safe_address)
        set(value) {
            binding.address.text = value
            field = value
        }


    fun setNewAddress(
        chain: Chain,
        newAddress: Solidity.Address,
        showChainPrefix: Boolean
    ) {

        address = newAddress

        with(binding) {
            this.errorMessage.visible(false, View.INVISIBLE)
            blockies.setAddress(newAddress)
            blockies.visible(true)
            address.text = newAddress.formatEthAddress(
                context,
                if (showChainPrefix) chain.shortName else null,
                addMiddleLinebreak = false
            )
            mainContainer.backgroundTintList = ColorStateList.valueOf(
                ResourcesCompat.getColor(
                    resources,
                    R.color.outline,
                    context.theme
                )
            )
        }
    }

    fun setError(
        chain: Chain,
        errorMessage: String?,
        input: String?,
        showChainPrefix: Boolean
    ) {

        address = null

        with(binding) {

            this.errorMessage.visible(true)
            this.errorMessage.text = errorMessage

            input?.asEthereumAddress()?.let {
                blockies.visible(true)
                blockies.setAddress(it)
                address.text = it.formatEthAddress(
                    context,
                    if (showChainPrefix) chain.shortName else null,
                    addMiddleLinebreak = false
                )
            } ?: run {
                if (input != null) {
                    blockies.setAddress(null)
                    blockies.visible(false)
                    if (input.isBlank()) {
                        address.setTextColor(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.label_tertiary,
                                context.theme
                            )
                        )
                        address.text = hint

                    } else {
                        address.setTextColor(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.label_primary,
                                context.theme
                            )
                        )
                        address.text = input
                    }
                }
            }

            mainContainer.backgroundTintList = ColorStateList.valueOf(
                ResourcesCompat.getColor(
                    resources,
                    R.color.error,
                    context.theme
                )
            )
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        binding.mainContainer.setOnClickListener(l)
    }
}
