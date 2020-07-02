package io.gnosis.safe.ui.transaction

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import io.gnosis.data.models.Transaction
import io.gnosis.data.models.TransactionStatus
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemTxChangeMastercopyBinding
import io.gnosis.safe.databinding.ItemTxQueuedChangeMastercopyBinding
import io.gnosis.safe.databinding.ItemTxQueuedSettingsChangeBinding
import io.gnosis.safe.databinding.ItemTxQueuedTransferBinding
import io.gnosis.safe.databinding.ItemTxSectionHeaderBinding
import io.gnosis.safe.databinding.ItemTxSettingsChangeBinding
import io.gnosis.safe.databinding.ItemTxTransferBinding
import io.gnosis.safe.ui.base.Adapter
import io.gnosis.safe.ui.base.BaseFactory
import io.gnosis.safe.ui.base.UnsupportedViewType
import io.gnosis.safe.utils.formatForTxList
import io.gnosis.safe.utils.shiftedString

enum class TransactionViewType {
    TRANSFER,
    TRANSFER_QUEUED,
    CHANGE_MASTERCOPY,
    CHANGE_MASTERCOPY_QUEUED,
    SETTINGS_CHANGE,
    SETTINGS_CHANGE_QUEUED,
    CUSTOM_TRANSACTION,
    CUSTOM_TRANSACTION_QUEUED,
    SECTION_HEADER
}

class TransactionViewHolderFactory : BaseFactory<BaseTransactionViewHolder<TransactionView>, TransactionView>() {

    @Suppress("UNCHECKED_CAST")
    override fun newViewHolder(viewBinding: ViewBinding, viewType: Int): BaseTransactionViewHolder<TransactionView> =
        when (viewType) {
            TransactionViewType.CHANGE_MASTERCOPY.ordinal -> ChangeMastercopyViewHolder(viewBinding as ItemTxChangeMastercopyBinding)
            TransactionViewType.CHANGE_MASTERCOPY_QUEUED.ordinal -> ChangeMastercopyQueuedViewHolder(viewBinding as ItemTxQueuedChangeMastercopyBinding)
            TransactionViewType.SETTINGS_CHANGE.ordinal -> SettingsChangeViewHolder(viewBinding as ItemTxSettingsChangeBinding)
            TransactionViewType.SETTINGS_CHANGE_QUEUED.ordinal -> SettingsChangeQueuedViewHolder(viewBinding as ItemTxQueuedSettingsChangeBinding)
            TransactionViewType.TRANSFER.ordinal -> TransferViewHolder(viewBinding as ItemTxTransferBinding)
            TransactionViewType.TRANSFER_QUEUED.ordinal -> TransferQueuedViewHolder(viewBinding as ItemTxQueuedTransferBinding)
            TransactionViewType.CUSTOM_TRANSACTION.ordinal -> CustomTransactionViewHolder(viewBinding as ItemTxTransferBinding)
            TransactionViewType.CUSTOM_TRANSACTION_QUEUED.ordinal -> CustomTransactionQueuedViewHolder(viewBinding as ItemTxQueuedTransferBinding)
            TransactionViewType.SECTION_HEADER.ordinal -> SectionHeaderViewHolder(viewBinding as ItemTxSectionHeaderBinding)
            else -> throw UnsupportedViewType(javaClass.name)
        } as BaseTransactionViewHolder<TransactionView>

    override fun layout(layoutInflater: LayoutInflater, parent: ViewGroup, viewType: Int): ViewBinding =
        when (viewType) {
            TransactionViewType.CHANGE_MASTERCOPY.ordinal -> ItemTxChangeMastercopyBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.CHANGE_MASTERCOPY_QUEUED.ordinal -> ItemTxQueuedChangeMastercopyBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SETTINGS_CHANGE.ordinal -> ItemTxSettingsChangeBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SETTINGS_CHANGE_QUEUED.ordinal -> ItemTxQueuedSettingsChangeBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.TRANSFER.ordinal,
            TransactionViewType.CUSTOM_TRANSACTION.ordinal -> ItemTxTransferBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.TRANSFER_QUEUED.ordinal,
            TransactionViewType.CUSTOM_TRANSACTION_QUEUED.ordinal -> ItemTxQueuedTransferBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SECTION_HEADER.ordinal -> ItemTxSectionHeaderBinding.inflate(layoutInflater, parent, false)
            else -> throw UnsupportedViewType(javaClass.name)
        }

    override fun viewTypeFor(item: TransactionView): Int =
        when (item) {
            is TransactionView.Transfer -> TransactionViewType.TRANSFER
            is TransactionView.TransferQueued -> TransactionViewType.TRANSFER_QUEUED
            is TransactionView.SettingsChange -> TransactionViewType.SETTINGS_CHANGE
            is TransactionView.SettingsChangeQueued -> TransactionViewType.SETTINGS_CHANGE_QUEUED
            is TransactionView.ChangeMastercopy -> TransactionViewType.CHANGE_MASTERCOPY
            is TransactionView.ChangeMastercopyQueued -> TransactionViewType.CHANGE_MASTERCOPY_QUEUED
            is TransactionView.SectionHeader -> TransactionViewType.SECTION_HEADER
            is TransactionView.CustomTransaction -> TransactionViewType.CUSTOM_TRANSACTION
            is TransactionView.CustomTransactionQueued -> TransactionViewType.CUSTOM_TRANSACTION_QUEUED
        }.ordinal
}

abstract class BaseTransactionViewHolder<T : TransactionView>(viewBinding: ViewBinding) : Adapter.ViewHolder<T>(viewBinding.root)

class TransferViewHolder(private val viewBinding: ItemTxTransferBinding) :
    BaseTransactionViewHolder<TransactionView.Transfer>(viewBinding) {
    private val resources = viewBinding.root.context.resources

    override fun bind(viewTransfer: TransactionView.Transfer, payloads: List<Any>) {
        with(viewBinding) {
            finalStatus.text = viewTransfer.transfer.status.name
            finalStatus.setTextColor(statusTextColor(viewTransfer.transfer.status, resources))
            amount.text = formatAmount(viewTransfer.transfer, viewTransfer.isIncoming)
            dateTime.text = viewTransfer.transfer.date
            if (viewTransfer.isIncoming) {
                txTypeIcon.setImageResource(R.drawable.ic_arrow_green_16dp)
                blockies.setAddress(viewTransfer.transfer.sender)
                ellipsizedAddress.text = viewTransfer.transfer.sender.formatForTxList()
                amount.setTextColor(resources.getColor(R.color.safe_green, null))
            } else {
                txTypeIcon.setImageResource(R.drawable.ic_arrow_red_10dp)
                blockies.setAddress(viewTransfer.transfer.recipient)
                ellipsizedAddress.text = viewTransfer.transfer.recipient.formatForTxList()
                amount.setTextColor(resources.getColor(R.color.gnosis_dark_blue, null))
            }
        }
    }
}

class TransferQueuedViewHolder(private val viewBinding: ItemTxQueuedTransferBinding) :
    BaseTransactionViewHolder<TransactionView.TransferQueued>(viewBinding) {
    private val resources = viewBinding.root.context.resources

    override fun bind(viewTransfer: TransactionView.TransferQueued, payloads: List<Any>) {
        with(viewBinding) {
            status.text = "\u2022 ${viewTransfer.transaction?.status?.name}"
            status.setTextColor(statusTextColor(viewTransfer.transfer.status, resources))
            amount.text = formatAmount(viewTransfer.transfer, viewTransfer.isIncoming)
            dateTime.text = viewTransfer.transfer.date
            if (viewTransfer.isIncoming) {
                txTypeIcon.setImageResource(R.drawable.ic_arrow_green_16dp)
                blockies.setAddress(viewTransfer.transfer.sender)
                ellipsizedAddress.text = viewTransfer.transfer.sender.formatForTxList()
                amount.setTextColor(resources.getColor(R.color.safe_green, null))
            } else {
                txTypeIcon.setImageResource(R.drawable.ic_arrow_red_10dp)
                blockies.setAddress(viewTransfer.transfer.recipient)
                ellipsizedAddress.text = viewTransfer.transfer.recipient.formatForTxList()
                amount.setTextColor(resources.getColor(R.color.gnosis_dark_blue, null))
            }
            if (viewTransfer.transfer.confirmations != null) {
                if (viewTransfer.transfer.confirmations!! >= viewTransfer.threshold) {
                    confirmations.setTextColor(resources.getColor(R.color.safe_green, null))
                    confirmationsIcon.setImageDrawable(resources.getDrawable(R.drawable.ic_confirmations_green_16dp, null))
                } else {
                    confirmations.setTextColor(resources.getColor(R.color.medium_grey, null))
                    confirmationsIcon.setImageDrawable(resources.getDrawable(R.drawable.ic_confirmations_grey_16dp, null))
                }
                confirmations.text = resources.getString(R.string.tx_list_confirmations, viewTransfer.transfer.confirmations, viewTransfer.threshold)
                confirmationsIcon.visibility = View.VISIBLE
                confirmations.visibility = View.VISIBLE
            } else {
                confirmations.visibility = View.INVISIBLE
                confirmationsIcon.visibility = View.INVISIBLE
            }
            nonce.text = viewTransfer.transfer.nonce.toString()
        }
    }
}

class SettingsChangeViewHolder(private val viewBinding: ItemTxSettingsChangeBinding) :
    BaseTransactionViewHolder<TransactionView.SettingsChange>(viewBinding) {
    private val resources = viewBinding.root.context.resources

    override fun bind(viewTransfer: TransactionView.SettingsChange, payloads: List<Any>) {
        with(viewBinding) {
            finalStatus.text = viewTransfer.transaction.status.name
            finalStatus.setTextColor(statusTextColor(viewTransfer.transaction.status, resources))
            dateTime.text = viewTransfer.transaction.date
            settingName.text = viewTransfer.transaction.dataDecoded.method
        }
    }
}

class SettingsChangeQueuedViewHolder(private val viewBinding: ItemTxQueuedSettingsChangeBinding) :
    BaseTransactionViewHolder<TransactionView.SettingsChangeQueued>(viewBinding) {
    private val resources = viewBinding.root.context.resources

    override fun bind(viewTransfer: TransactionView.SettingsChangeQueued, payloads: List<Any>) {
        with(viewBinding) {
            status.text = "\u2022 ${viewTransfer.transaction.status.name}"
            status.setTextColor(statusTextColor(viewTransfer.transaction.status, resources))
            dateTime.text = viewTransfer.transaction.date

            if (viewTransfer.transaction.confirmations != null) {
                if (viewTransfer.transaction.confirmations!! >= viewTransfer.threshold) {
                    confirmations.setTextColor(resources.getColor(R.color.safe_green, null))
                    confirmationsIcon.setImageDrawable(resources.getDrawable(R.drawable.ic_confirmations_green_16dp, null))
                } else {
                    confirmations.setTextColor(resources.getColor(R.color.medium_grey, null))
                    confirmationsIcon.setImageDrawable(resources.getDrawable(R.drawable.ic_confirmations_grey_16dp, null))
                }
                confirmations.text = resources.getString(R.string.tx_list_confirmations, viewTransfer.transaction.confirmations, viewTransfer.threshold)
                confirmations.visibility = View.VISIBLE
                confirmationsIcon.visibility = View.VISIBLE
            } else {
                confirmations.visibility = View.INVISIBLE
                confirmationsIcon.visibility = View.INVISIBLE
            }
        }
    }
}

class ChangeMastercopyViewHolder(private val viewBinding: ItemTxChangeMastercopyBinding) :
    BaseTransactionViewHolder<TransactionView.ChangeMastercopy>(viewBinding) {
    private val resources = viewBinding.root.context.resources

    override fun bind(viewTransfer: TransactionView.ChangeMastercopy, payloads: List<Any>) {
        with(viewBinding) {
            finalStatus.text = viewTransfer.transaction.status.name
            finalStatus.setTextColor(statusTextColor(viewTransfer.transaction.status, resources))

            dateTime.text = viewTransfer.transaction.date
        }
    }
}

class ChangeMastercopyQueuedViewHolder(private val viewBinding: ItemTxQueuedChangeMastercopyBinding) :
    BaseTransactionViewHolder<TransactionView.SettingsChangeQueued>(viewBinding) {
    private val resources = viewBinding.root.context.resources

    override fun bind(viewTransfer: TransactionView.SettingsChangeQueued, payloads: List<Any>) {
        with(viewBinding) {
            status.text = "\u2022 ${viewTransfer.transaction.status.name}"
            status.setTextColor(statusTextColor(viewTransfer.transaction.status, resources))

            dateTime.text = viewTransfer.transaction.date

            if (viewTransfer.transaction.confirmations != null) {
                confirmations.text = resources.getString(R.string.tx_list_confirmations, viewTransfer.transaction.confirmations, viewTransfer.threshold)
                confirmationsIcon.visibility = View.VISIBLE
            } else {
                confirmations.visibility = View.INVISIBLE
                confirmationsIcon.visibility = View.INVISIBLE
            }
        }
    }
}

class CustomTransactionQueuedViewHolder(private val viewBinding: ItemTxQueuedTransferBinding) :
    BaseTransactionViewHolder<TransactionView.CustomTransactionQueued>(viewBinding) {
    private val resources = viewBinding.root.context.resources

    override fun bind(viewTransfer: TransactionView.CustomTransactionQueued, payloads: List<Any>) {
        with(viewBinding) {
            txTypeIcon.setImageResource(R.drawable.ic_code)

            status.text = "\u2022 ${viewTransfer.transaction.status.name}"
            status.setTextColor(statusTextColor(viewTransfer.transaction.status, resources))

            dateTime.text = viewTransfer.transaction.date

            if (viewTransfer.transaction.confirmations != null) {
                confirmations.text = resources.getString(R.string.tx_list_confirmations, viewTransfer.transaction.confirmations, viewTransfer.threshold)
                confirmationsIcon.visibility = View.VISIBLE
            } else {
                confirmations.visibility = View.INVISIBLE
                confirmationsIcon.visibility = View.INVISIBLE
            }

            blockies.setAddress(viewTransfer.transaction.address)
            ellipsizedAddress.text = viewTransfer.transaction.address.formatForTxList()
        }
    }
}

class CustomTransactionViewHolder(private val viewBinding: ItemTxTransferBinding) :
    BaseTransactionViewHolder<TransactionView.CustomTransaction>(viewBinding) {
    private val resources = viewBinding.root.context.resources

    override fun bind(viewTransfer: TransactionView.CustomTransaction, payloads: List<Any>) {
        with(viewBinding) {
            txTypeIcon.setImageResource(R.drawable.ic_code)

            finalStatus.text = viewTransfer.transaction.status.name
            finalStatus.setTextColor(statusTextColor(viewTransfer.transaction.status, resources))

            dateTime.text = viewTransfer.transaction.date

            blockies.setAddress(viewTransfer.transaction.address)
            ellipsizedAddress.text = viewTransfer.transaction.address.formatForTxList()
        }
    }
}

class SectionHeaderViewHolder(private val viewBinding: ItemTxSectionHeaderBinding) :
    BaseTransactionViewHolder<TransactionView.SectionHeader>(viewBinding) {

    override fun bind(sectionHeader: TransactionView.SectionHeader, payloads: List<Any>) {
        with(viewBinding) {
            sectionTitle.setText(sectionHeader.title)
        }
    }
}

private fun statusTextColor(status: TransactionStatus, resources: Resources): Int {
    return when (status) {
        TransactionStatus.Success -> resources.getColor(R.color.safe_green, null)
        TransactionStatus.Cancelled -> resources.getColor(R.color.dark_grey, null)
        else -> resources.getColor(R.color.safe_failed_red, null)
    }
}

private fun formatAmount(viewTransfer: Transaction.Transfer, incoming: Boolean): String {
    val inOut = if (incoming) "+" else "-"
    val symbol = viewTransfer.tokenInfo?.symbol
    val value: String = viewTransfer.tokenInfo?.decimals?.let { viewTransfer.value.shiftedString(decimals = it) }.toString()
    return "%s%s %s".format(inOut, value, symbol)
}
