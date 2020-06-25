package io.gnosis.safe.ui.transaction.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemTxLoadStateBinding
import pm.gnosis.svalinn.common.utils.visible

class TransactionLoadStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<TransactionLoadStateViewHolder>() {

    override fun onBindViewHolder(holder: TransactionLoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): TransactionLoadStateViewHolder {
        return TransactionLoadStateViewHolder.create(parent, retry)
    }
}

class TransactionLoadStateViewHolder(
    private val binding: ItemTxLoadStateBinding,
    retry: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.retryButton.setOnClickListener { retry.invoke() }
    }

    fun bind(loadState: LoadState) {
       with(binding) {
           progressBar.visible(loadState is LoadState.Loading)
           retryButton.visible(loadState !is LoadState.Loading)
           errorMsg.visible(loadState !is LoadState.Loading)
       }

    }

    companion object {

        fun create(parent: ViewGroup, retry: () -> Unit): TransactionLoadStateViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tx_load_state, parent, false)
            val binding = ItemTxLoadStateBinding.bind(view)
            return TransactionLoadStateViewHolder(binding, retry)
        }
    }
}
