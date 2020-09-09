package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewTxStatusBinding
import pm.gnosis.svalinn.common.utils.visible

class TxStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewTxStatusBinding.inflate(LayoutInflater.from(context), this) }


    fun setStatus(
        @StringRes titleRes: Int,
        @DrawableRes iconRes: Int,
        @StringRes statusTextRes: Int,
        @ColorRes statusColorRes: Int
    ) {

        binding.type.setText(titleRes)
        binding.typeIcon.setImageResource(iconRes)

        if (statusTextRes == R.string.tx_status_status_awaiting_confirmations || statusTextRes == R.string.tx_status_status_awaiting_execution) {
            binding.statusLong.setText(statusTextRes)
            binding.statusLong.setTextColor(ContextCompat.getColor(context, statusColorRes))
            binding.statusLong.visible(true)
            binding.status.visibility = View.GONE

        } else {
            binding.status.setText(statusTextRes)
            binding.status.setTextColor(ContextCompat.getColor(context, statusColorRes))
            binding.status.visible(true)
            binding.statusLong.visibility = View.GONE
        }
    }

    enum class TxType(@DrawableRes val iconRes: Int, @StringRes val titleRes: Int) {
        TRANSFER_INCOMING(R.drawable.ic_arrow_green_10dp, R.string.tx_status_type_transfer_incoming),
        TRANSFER_OUTGOING(R.drawable.ic_arrow_red_10dp, R.string.tx_status_type_transfer_outgoing),
        MODIFY_SETTINGS(R.drawable.ic_settings_change_14dp, R.string.tx_status_type_modify_settings),
        CUSTOM(R.drawable.ic_code_16dp, R.string.tx_status_type_custom),
        CREATION(R.drawable.ic_settings_change_14dp, R.string.tx_status_type_creation)
    }
}
