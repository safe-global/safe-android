package io.gnosis.safe.ui.transactions.execution.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.safe.databinding.ViewSelectedKeyItemBinding
import io.gnosis.safe.ui.settings.owner.list.OwnerViewData
import io.gnosis.safe.utils.imageRes16dp
import pm.gnosis.svalinn.common.utils.visible

class SelectedKeyItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewSelectedKeyItemBinding.inflate(LayoutInflater.from(context), this)

    fun setKey(keyData: OwnerViewData, balance: String? = null) {
        with(binding) {
            keyName.text = keyData.name
            blockies.setAddress(keyData.address)
            keyType.setImageResource(keyData.type.imageRes16dp())
            keyDataContainer.visible(true)
            keyBalance.text = balance
            keyBalance.visible(!balance.isNullOrBlank())
        }
    }
}
