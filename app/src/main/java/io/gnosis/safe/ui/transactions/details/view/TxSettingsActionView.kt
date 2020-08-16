package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.data.models.TransactionInfo
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewTxActionBinding

class TxSettingsActionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewTxActionBinding.inflate(LayoutInflater.from(context), this) }

    private var collapsed: Boolean = true

    fun setData(txInfo: TransactionInfo) {

        binding.actionTitle.text = txInfo.toString()



    }


}
