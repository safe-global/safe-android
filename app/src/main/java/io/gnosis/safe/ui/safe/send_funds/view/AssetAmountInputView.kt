package io.gnosis.safe.ui.safe.send_funds.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.safe.databinding.ViewAssetAmountInputBinding
import io.gnosis.safe.utils.loadTokenLogo

class AssetAmountInputView@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewAssetAmountInputBinding.inflate(LayoutInflater.from(context), this)

    fun setAssetLogo(logoUri: String?) {
        binding.assetLogo.loadTokenLogo(icon = logoUri)
    }
}
