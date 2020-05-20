package io.gnosis.safe.ui.transaction

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import io.gnosis.data.models.Transaction
import io.gnosis.safe.databinding.*
import io.gnosis.safe.ui.base.BaseFactory
import io.gnosis.safe.ui.base.Adapter
import io.gnosis.safe.ui.base.UnsupportedViewType

enum class TransactionViewType {
    CHANGE_MASTERCOPY, CHANGE_MASTERCOPY_QUEUED, SETTINGS_CHANGE, SETTINGS_CHANGE_QUEUED, TRANSFER, TRANSFER_QUEUED
}

class TransactionViewHolderFactory : BaseFactory<BaseTransactionViewHolder<Transaction>, Transaction>() {

    @Suppress("UNCHECKED_CAST")
    override fun newViewHolder(viewBinding: ViewBinding, viewType: Int): BaseTransactionViewHolder<Transaction> =
        when (viewType) {
            TransactionViewType.CHANGE_MASTERCOPY.ordinal -> ChangeMastercopyViewHolder(viewBinding as ItemTxChangeMastercopyBinding)
            TransactionViewType.CHANGE_MASTERCOPY_QUEUED.ordinal -> ChangeMastercopyQueuedViewHolder(viewBinding as ItemTxQueuedChangeMastercopyBinding)
            TransactionViewType.SETTINGS_CHANGE.ordinal -> SettingsChangeViewHolder(viewBinding as ItemTxSettingsChangeBinding)
            TransactionViewType.SETTINGS_CHANGE_QUEUED.ordinal -> SettingsChangeQueuedViewHolder(viewBinding as ItemTxQueuedSettingsChangeBinding)
            TransactionViewType.TRANSFER.ordinal -> TransferViewHolder(viewBinding as ItemTxTransferBinding)
            TransactionViewType.TRANSFER_QUEUED.ordinal -> TransferQueuedViewHolder(viewBinding as ItemTxQueuedTransferBinding)
            else -> throw UnsupportedViewType(javaClass.name)
        } as BaseTransactionViewHolder<Transaction>


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

    override fun viewTypeFor(item: Transaction): Int =
        when (item) {
            is Transaction.ChangeMastercopy -> TransactionViewType.CHANGE_MASTERCOPY
            is Transaction.SettingsChange -> TransactionViewType.SETTINGS_CHANGE
            is Transaction.Transfer -> TransactionViewType.TRANSFER
            is Transaction.ChangeMastercopyQueued -> TransactionViewType.CHANGE_MASTERCOPY_QUEUED
            is Transaction.SettingsChangeQueued -> TransactionViewType.SETTINGS_CHANGE_QUEUED
            is Transaction.TransferQueued -> TransactionViewType.TRANSFER_QUEUED
            else -> throw UnsupportedViewType(javaClass.name)
        }.ordinal
}


abstract class BaseTransactionViewHolder<T : Transaction>(viewBinding: ViewBinding) : Adapter.ViewHolder<T>(viewBinding.root)

class ChangeMastercopyViewHolder(viewBinding: ItemTxChangeMastercopyBinding) :
    BaseTransactionViewHolder<Transaction.ChangeMastercopy>(viewBinding) {

    override fun bind(data: Transaction.ChangeMastercopy, payloads: List<Any>) {
        TODO("Not yet implemented")
    }
}

class SettingsChangeViewHolder(viewBinding: ItemTxSettingsChangeBinding) :
    BaseTransactionViewHolder<Transaction.SettingsChange>(viewBinding) {

    override fun bind(data: Transaction.SettingsChange, payloads: List<Any>) {
        TODO("Not yet implemented")
    }
}

class TransferViewHolder(viewBinding: ItemTxTransferBinding) :
    BaseTransactionViewHolder<Transaction.Transfer>(viewBinding) {

    override fun bind(data: Transaction.Transfer, payloads: List<Any>) {
        TODO("Not yet implemented")
    }
}

class ChangeMastercopyQueuedViewHolder(viewBinding: ItemTxQueuedChangeMastercopyBinding) :
    BaseTransactionViewHolder<Transaction.SettingsChangeQueued>(viewBinding) {

    override fun bind(data: Transaction.SettingsChangeQueued, payloads: List<Any>) {
        TODO("Not yet implemented")
    }
}

class SettingsChangeQueuedViewHolder(viewBinding: ItemTxQueuedSettingsChangeBinding) :
    BaseTransactionViewHolder<Transaction.SettingsChangeQueued>(viewBinding) {

    override fun bind(data: Transaction.SettingsChangeQueued, payloads: List<Any>) {
        TODO("Not yet implemented")
    }
}

class TransferQueuedViewHolder(viewBinding: ItemTxQueuedTransferBinding) :
    BaseTransactionViewHolder<Transaction.TransferQueued>(viewBinding) {

    override fun bind(data: Transaction.TransferQueued, payloads: List<Any>) {
        TODO("Not yet implemented")
    }
}
