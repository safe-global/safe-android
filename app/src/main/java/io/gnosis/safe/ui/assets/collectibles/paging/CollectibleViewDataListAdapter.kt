package io.gnosis.safe.ui.assets.collectibles.paging

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import io.gnosis.safe.ui.assets.collectibles.BaseCollectiblesViewHolder
import io.gnosis.safe.ui.assets.collectibles.CollectibleViewData
import io.gnosis.safe.ui.base.adapter.BaseFactory
import io.gnosis.safe.ui.base.adapter.UnsupportedViewType

class CollectibleViewDataListAdapter(
    private val factory: BaseFactory<BaseCollectiblesViewHolder<CollectibleViewData>, CollectibleViewData>
) : PagingDataAdapter<CollectibleViewData, BaseCollectiblesViewHolder<CollectibleViewData>>(COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseCollectiblesViewHolder<CollectibleViewData> =
        factory.newViewHolder(
            factory.layout(LayoutInflater.from(parent.context), parent, viewType),
            viewType
        )

    override fun onBindViewHolder(holder: BaseCollectiblesViewHolder<CollectibleViewData>, position: Int) {
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

        private val COMPARATOR = object : DiffUtil.ItemCallback<CollectibleViewData>() {

            override fun areItemsTheSame(oldItem: CollectibleViewData, newItem: CollectibleViewData): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: CollectibleViewData, newItem: CollectibleViewData): Boolean {
                return oldItem == newItem
            }
        }
    }
}
