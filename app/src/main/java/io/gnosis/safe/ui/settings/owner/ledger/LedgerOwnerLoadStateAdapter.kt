package io.gnosis.safe.ui.settings.owner.ledger

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemOwnerLoadStateBinding
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber


class LedgerOwnerLoadStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<LedgerOwnerLoadStateViewHolder>() {

    override fun onBindViewHolder(holder: LedgerOwnerLoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LedgerOwnerLoadStateViewHolder {
        return LedgerOwnerLoadStateViewHolder.create(parent, retry)
    }
}

class LedgerOwnerLoadStateViewHolder(
    private val binding: ItemOwnerLoadStateBinding,
    retry: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.retryButton.setOnClickListener { retry.invoke() }
    }

    fun bind(loadState: LoadState) {
        Timber.i("----> loadState: $loadState")
        with(binding) {
            progressBar.visible(loadState is LoadState.Loading)
            retryButton.visible(loadState !is LoadState.Loading)
            errorMsg.visible(loadState !is LoadState.Loading)
        }

    }

    companion object {

        fun create(parent: ViewGroup, retry: () -> Unit): LedgerOwnerLoadStateViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tx_load_state, parent, false)
            val binding = ItemOwnerLoadStateBinding.bind(view)
            return LedgerOwnerLoadStateViewHolder(binding, retry)
        }
    }
}
