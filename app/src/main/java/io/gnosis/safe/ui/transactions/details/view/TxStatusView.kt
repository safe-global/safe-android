package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import io.gnosis.data.models.TransactionStatus
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewTxStatusBinding
import pm.gnosis.svalinn.common.utils.visible

class TxStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewTxStatusBinding.inflate(LayoutInflater.from(context), this) }

    fun setStatus(txType: TxType, status: TransactionStatus) {

        binding.type.setText(txType.titleRes)
        binding.typeIcon.setImageResource(txType.iconRes)

        when (status) {
            TransactionStatus.AWAITING_CONFIRMATIONS -> {
                binding.statusLong.setText(R.string.tx_status_status_awaiting_confirmations)
                binding.statusLong.setTextColor(ContextCompat.getColor(context, R.color.safe_pending_orange))
                binding.statusLong.visible(true)

                binding.status.visible(false)
            }
            TransactionStatus.AWAITING_EXECUTION -> {
                binding.statusLong.setText(R.string.tx_status_status_awaiting_execution)
                binding.statusLong.setTextColor(ContextCompat.getColor(context, R.color.safe_pending_orange))
                binding.statusLong.visible(true)

                binding.status.visible(false)
            }
            TransactionStatus.SUCCESS -> {
                binding.status.setText(R.string.tx_status_status_success)
                binding.status.setTextColor(ContextCompat.getColor(context, R.color.safe_green))
                binding.status.visible(true)

                binding.statusLong.visible(false)
            }
            TransactionStatus.CANCELLED -> {
                binding.status.setText(R.string.tx_status_status_cancelled)
                binding.status.setTextColor(ContextCompat.getColor(context, R.color.dark_grey))
                binding.status.visible(true)

                binding.statusLong.visible(false)
            }
            TransactionStatus.FAILED -> {
                binding.status.setText(R.string.tx_status_status_failed)
                binding.status.setTextColor(ContextCompat.getColor(context, R.color.tomato))
                binding.status.visible(true)

                binding.statusLong.visible(false)
            }
            TransactionStatus.PENDING -> {
                binding.status.setText(R.string.tx_status_status_pending)
                binding.status.setTextColor(ContextCompat.getColor(context, R.color.dark_grey))
                binding.status.visible(true)

                binding.statusLong.visible(false)
            }
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
