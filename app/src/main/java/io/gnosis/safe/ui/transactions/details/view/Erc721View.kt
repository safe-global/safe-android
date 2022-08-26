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

    fun setToken(logoUri: String, nftId: String?, outgoing: Boolean, amount: String?) {
        with(binding) {
            logo.loadTokenLogo(logoUri, R.drawable.ic_nft_placeholder)
            tokenId.text = nftId

            if (outgoing) {
                tokenAmount.text = amount
                tokenAmount.setTextColor(context.getColorCompat(R.color.label_primary))
            } else {
                tokenAmount.text = amount
                tokenAmount.setTextColor(context.getColorCompat(R.color.primary))
            }
        }
    }
}
