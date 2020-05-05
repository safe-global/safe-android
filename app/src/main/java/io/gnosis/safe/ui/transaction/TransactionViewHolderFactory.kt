package io.gnosis.safe.ui.transaction

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import io.gnosis.data.models.Transaction
import io.gnosis.safe.databinding.EntryTransactionCancelledBinding
import io.gnosis.safe.databinding.EntryTransactionFailedBinding
import io.gnosis.safe.databinding.EntryTransactionPendingBinding
import io.gnosis.safe.databinding.EntryTransactionSuccessBinding
import io.gnosis.safe.ui.adapter.BaseFactory
import io.gnosis.safe.ui.adapter.BaseViewHolder
import io.gnosis.safe.ui.adapter.UnsupportedViewType

enum class TransactionViewType {
    PENDING, SUCCESS, CANCELLED, FAILED
}

class TransactionViewHolderFactory : BaseFactory<BaseTransactionViewHolder<Transaction>>() {

    override fun newViewHolder(viewBinding: ViewBinding, viewType: Int): BaseTransactionViewHolder<Transaction> =
        when (viewType) {
            TransactionViewType.PENDING.ordinal -> PendingViewHolder(viewBinding as EntryTransactionPendingBinding)
            TransactionViewType.SUCCESS.ordinal -> SuccessViewHolder(viewBinding as EntryTransactionSuccessBinding)
            TransactionViewType.CANCELLED.ordinal -> CancelledViewHolder(viewBinding as EntryTransactionCancelledBinding)
            TransactionViewType.FAILED.ordinal -> FailedViewHolder(viewBinding as EntryTransactionFailedBinding)
            else -> throw UnsupportedViewType(javaClass.name)
        } as BaseTransactionViewHolder<Transaction>


    override fun layout(layoutInflater: LayoutInflater, parent: ViewGroup, viewType: Int): ViewBinding =
        when (viewType) {
            TransactionViewType.PENDING.ordinal -> EntryTransactionPendingBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SUCCESS.ordinal -> EntryTransactionSuccessBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.CANCELLED.ordinal -> EntryTransactionCancelledBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.FAILED.ordinal -> EntryTransactionFailedBinding.inflate(layoutInflater, parent, false)
            else -> throw UnsupportedViewType(javaClass.name)
        }

    override fun <T> viewTypeFor(item: T): Int =
        when (item) {
            is Transaction.Pending -> TransactionViewType.PENDING
            is Transaction.Success -> TransactionViewType.SUCCESS
            is Transaction.Cancelled -> TransactionViewType.CANCELLED
            is Transaction.Failed -> TransactionViewType.FAILED
            else -> throw UnsupportedViewType(javaClass.name)
        }.ordinal
}


abstract class BaseTransactionViewHolder<T : Transaction>(viewBinding: ViewBinding) : BaseViewHolder<T>(viewBinding)

class FailedViewHolder(viewBinding: EntryTransactionFailedBinding) : BaseTransactionViewHolder<Transaction.Pending>(viewBinding) {

    override fun bind(item: Transaction.Pending) {
    }
}

class CancelledViewHolder(viewBinding: EntryTransactionCancelledBinding) : BaseTransactionViewHolder<Transaction.Cancelled>(viewBinding) {

    override fun bind(item: Transaction.Cancelled) {
    }
}

class SuccessViewHolder(viewBinding: EntryTransactionSuccessBinding) : BaseTransactionViewHolder<Transaction.Success>(viewBinding) {

    override fun bind(item: Transaction.Success) {
    }
}

class PendingViewHolder(viewBinding: EntryTransactionPendingBinding) : BaseTransactionViewHolder<Transaction.Pending>(viewBinding) {

    override fun bind(item: Transaction.Pending) {
    }
}

