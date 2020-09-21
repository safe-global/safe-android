package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewTxErc721Binding
import io.gnosis.safe.utils.loadTokenLogo
import pm.gnosis.svalinn.common.utils.getColorCompat

class Erc721View @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewTxErc721Binding.inflate(LayoutInflater.from(context), this) }

    fun setToken(logoUri: String, tokenId: String?, outgoing: Boolean, amount: String?) {
        binding.logo.loadTokenLogo(logoUri, R.drawable.ic_nft_placeholder)
        binding.tokenId.text = tokenId

        if (outgoing) {
            binding.tokenAmount.text = amount
            binding.tokenAmount.setTextColor(context.getColorCompat(R.color.gnosis_dark_blue))
        } else {
            binding.tokenAmount.text = amount
            binding.tokenAmount.setTextColor(context.getColorCompat(R.color.safe_green))
        }
    }
}
