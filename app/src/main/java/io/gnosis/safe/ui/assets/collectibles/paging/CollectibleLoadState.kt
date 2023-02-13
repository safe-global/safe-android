package io.gnosis.safe.ui.assets.collectibles.paging

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemCollectibleLoadStateBinding
import pm.gnosis.svalinn.common.utils.visible

class CollectibleLoadStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<CollectibleLoadStateViewHolder>() {

    override fun onBindViewHolder(holder: CollectibleLoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): CollectibleLoadStateViewHolder {
        return CollectibleLoadStateViewHolder.create(parent, retry)
    }
}

class CollectibleLoadStateViewHolder(
    private val binding: ItemCollectibleLoadStateBinding,
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

        fun create(parent: ViewGroup, retry: () -> Unit): CollectibleLoadStateViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_collectible_load_state, parent, false)
            val binding = ItemCollectibleLoadStateBinding.bind(view)
            return CollectibleLoadStateViewHolder(binding, retry)
        }
    }
}
