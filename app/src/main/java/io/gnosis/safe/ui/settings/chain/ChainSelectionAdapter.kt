package io.gnosis.safe.ui.settings.chain

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.gnosis.data.models.Chain
import io.gnosis.safe.databinding.ItemChainBinding
import io.gnosis.safe.databinding.ItemChainTitleBinding
import io.gnosis.safe.ui.base.adapter.UnsupportedViewType

class ChainSelectionAdapter : PagingDataAdapter<ChainsViewData, BaseItemViewHolder>(COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseItemViewHolder {
        return when (ItemViewType.values()[viewType]) {
            ItemViewType.TITLE -> TitleItemViewHolder(ItemChainTitleBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            ItemViewType.CHAIN -> ChainItemViewHolder(ItemChainBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: BaseItemViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is ChainItemViewHolder -> {
                val chainItem = item as ChainsViewData.ChainItem
                holder.bind(chainItem.chain)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item) {
            is ChainsViewData.TitleItem -> ItemViewType.TITLE
            is ChainsViewData.ChainItem -> ItemViewType.CHAIN
            else -> throw UnsupportedViewType()
        }.ordinal
    }

    enum class ItemViewType {
        TITLE,
        CHAIN
    }

    companion object {

        private val COMPARATOR = object : DiffUtil.ItemCallback<ChainsViewData>() {

            override fun areItemsTheSame(oldItem: ChainsViewData, newItem: ChainsViewData): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: ChainsViewData, newItem: ChainsViewData): Boolean {
                return oldItem == newItem
            }
        }
    }
}

abstract class BaseItemViewHolder(
    viewBinding: ViewBinding
) : RecyclerView.ViewHolder(viewBinding.root)

class ChainItemViewHolder(private val binding: ItemChainBinding) : BaseItemViewHolder(binding) {

    fun bind(chain: Chain) {
        with(binding) {
            name.text = chain.name
            root.setOnClickListener {

            }
        }
    }
}

class TitleItemViewHolder(binding: ItemChainTitleBinding) : BaseItemViewHolder(binding)
