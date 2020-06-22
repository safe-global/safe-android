package io.gnosis.safe.ui.transaction

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import io.gnosis.safe.R
import io.gnosis.safe.databinding.*
import io.gnosis.safe.ui.base.Adapter
import io.gnosis.safe.ui.base.BaseFactory
import io.gnosis.safe.ui.base.UnsupportedViewType
import io.gnosis.safe.utils.asMiddleEllipsized
import io.gnosis.safe.utils.formatForTxList
import io.gnosis.safe.utils.shiftedString
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity

enum class TransactionViewType {
    CHANGE_MASTERCOPY, CHANGE_MASTERCOPY_QUEUED, SETTINGS_CHANGE, SETTINGS_CHANGE_QUEUED, TRANSFER, TRANSFER_QUEUED
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
            else -> throw UnsupportedViewType(javaClass.name)
        } as BaseTransactionViewHolder<TransactionView>

    override fun layout(layoutInflater: LayoutInflater, parent: ViewGroup, viewType: Int): ViewBinding =
        when (viewType) {
            TransactionViewType.CHANGE_MASTERCOPY.ordinal -> ItemTxChangeMastercopyBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.CHANGE_MASTERCOPY_QUEUED.ordinal -> ItemTxQueuedChangeMastercopyBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SETTINGS_CHANGE.ordinal -> ItemTxSettingsChangeBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SETTINGS_CHANGE_QUEUED.ordinal -> ItemTxQueuedSettingsChangeBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.TRANSFER.ordinal -> ItemTxTransferBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.TRANSFER_QUEUED.ordinal -> ItemTxQueuedTransferBinding.inflate(layoutInflater, parent, false)
            else -> throw UnsupportedViewType(javaClass.name)
        }

    override fun viewTypeFor(item: TransactionView): Int =
        when (item) {
            is TransactionView.ChangeMastercopy -> TransactionViewType.CHANGE_MASTERCOPY
            is TransactionView.SettingsChange -> TransactionViewType.SETTINGS_CHANGE
            is TransactionView.Transfer -> TransactionViewType.TRANSFER
            is TransactionView.ChangeMastercopyQueued -> TransactionViewType.CHANGE_MASTERCOPY_QUEUED
            is TransactionView.SettingsChangeQueued -> TransactionViewType.SETTINGS_CHANGE_QUEUED
            is TransactionView.TransferQueued -> TransactionViewType.TRANSFER_QUEUED
            else -> throw UnsupportedViewType(javaClass.name)
        }.ordinal
}


abstract class BaseTransactionViewHolder<T : TransactionView>(viewBinding: ViewBinding) : Adapter.ViewHolder<T>(viewBinding.root)

class ChangeMastercopyViewHolder(viewBinding: ItemTxChangeMastercopyBinding) :
    BaseTransactionViewHolder<TransactionView.ChangeMastercopy>(viewBinding) {

    override fun bind(data: TransactionView.ChangeMastercopy, payloads: List<Any>) {
        TODO("Not yet implemented")
    }
}

class SettingsChangeViewHolder(private val viewBinding: ItemTxSettingsChangeBinding) :
    BaseTransactionViewHolder<TransactionView.SettingsChange>(viewBinding) {

    override fun bind(data: TransactionView.SettingsChange, payloads: List<Any>) {
        with(viewBinding) {
            settingName.text = data.transaction.dataDecoded.method
            dateTime.text = data.transaction.date
        }
    }
}

class TransferViewHolder(private val viewBinding: ItemTxTransferBinding) :
    BaseTransactionViewHolder<TransactionView.Transfer>(viewBinding) {

    override fun bind(data: TransactionView.Transfer, payloads: List<Any>) {
        with(viewBinding) {
            val value = data.transaction.value
            amount.text = "${value.shiftedString(18)} ${data.transaction.tokenInfo?.symbol}"
            dateTime.text = data.transaction.date
            if (data.isIncoming) {
                txTypeIcon.setImageResource(R.drawable.ic_arrow_green_16dp)
                blockies.setAddress(data.transaction.sender)
                ellipsizedAddress.text = data.transaction.sender.formatForTxList()
            } else {
                txTypeIcon.setImageResource(R.drawable.ic_arrow_red_10dp)
                blockies.setAddress(data.transaction.recipient)
                ellipsizedAddress.text = data.transaction.recipient.formatForTxList()
            }
        }
    }
}

class ChangeMastercopyQueuedViewHolder(viewBinding: ItemTxQueuedChangeMastercopyBinding) :
    BaseTransactionViewHolder<TransactionView.SettingsChangeQueued>(viewBinding) {

    override fun bind(data: TransactionView.SettingsChangeQueued, payloads: List<Any>) {
        TODO("Not yet implemented")
    }
}

class SettingsChangeQueuedViewHolder(viewBinding: ItemTxQueuedSettingsChangeBinding) :
    BaseTransactionViewHolder<TransactionView.SettingsChangeQueued>(viewBinding) {

    override fun bind(data: TransactionView.SettingsChangeQueued, payloads: List<Any>) {
        TODO("Not yet implemented")
    }
}

class TransferQueuedViewHolder(viewBinding: ItemTxQueuedTransferBinding) :
    BaseTransactionViewHolder<TransactionView.TransferQueued>(viewBinding) {

    override fun bind(data: TransactionView.TransferQueued, payloads: List<Any>) {
        TODO("Not yet implemented")
    }
}
