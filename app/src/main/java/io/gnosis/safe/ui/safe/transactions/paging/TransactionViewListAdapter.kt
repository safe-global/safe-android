package io.gnosis.safe.ui.safe.transactions.paging

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import io.gnosis.safe.ui.base.BaseFactory
import io.gnosis.safe.ui.base.UnsupportedViewType
import io.gnosis.safe.ui.safe.transactions.BaseTransactionViewHolder
import io.gnosis.safe.ui.safe.transactions.TransactionView

class TransactionViewListAdapter(
    private val factory: BaseFactory<BaseTransactionViewHolder<TransactionView>, TransactionView>
) : PagingDataAdapter<TransactionView, BaseTransactionViewHolder<TransactionView>>(COMPARATOR)  {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseTransactionViewHolder<TransactionView> =
        factory.newViewHolder(
            factory.layout(LayoutInflater.from(parent.context), parent, viewType),
            viewType
        )

    override fun onBindViewHolder(holder: BaseTransactionViewHolder<TransactionView>, position: Int) {
        val uiModel = getItem(position)
        holder.bind(uiModel!!, listOf())
    }

    override fun getItemViewType(position: Int): Int {
        val uiModel = getItem(position)
        return uiModel?.let {
            factory.viewTypeFor(it).takeUnless { it < 0 } ?: throw UnsupportedViewType()
        } ?: throw UnsupportedViewType()
    }

    companion object {

        private val COMPARATOR = object : DiffUtil.ItemCallback<TransactionView>() {

            override fun areItemsTheSame(oldItem: TransactionView, newItem: TransactionView): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: TransactionView, newItem: TransactionView): Boolean {
                return oldItem == newItem
            }
        }
    }
}
