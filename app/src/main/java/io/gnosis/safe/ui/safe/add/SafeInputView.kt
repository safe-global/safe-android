package io.gnosis.safe.ui.safe.add

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewSafeInputBinding
import io.gnosis.safe.utils.formatEthAddress
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible

class SafeInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewSafeInputBinding.inflate(LayoutInflater.from(context), this)

    var address: Solidity.Address? = null
        set(value) {
            setErrorState(false)
            if (value != null) setNewAddress(value)
            else setHint()
            field = value
        }

    var error: String? = null
        set(value) {
            setErrorState(value != null, value)
            field = value
        }

    override fun setOnClickListener(l: OnClickListener?) {
        binding.mainContainer.setOnClickListener(l)
    }

    private fun setErrorState(toggle: Boolean, errorMessage: String? = null) {
        with(binding) {
            this.errorMessage.visible(toggle, View.INVISIBLE)
            this.errorMessage.text = errorMessage
            val colorId = if (toggle) R.color.tomato else R.color.white_smoke
            mainContainer.backgroundTintList = ColorStateList.valueOf(ResourcesCompat.getColor(resources, colorId, context.theme))
        }
    }

    private fun setNewAddress(newAddress: Solidity.Address) {
        with(binding) {
            blockies.setAddress(newAddress)
            blockies.visible(true)
            address.text = newAddress.formatEthAddress(context, addMiddleLinebreak = false)
        }
    }

    private fun setHint() {
        with(binding) {
            blockies.setAddress(null)
            blockies.visible(false)
            address.setText(R.string.enter_safe_address)
        }
    }
}
