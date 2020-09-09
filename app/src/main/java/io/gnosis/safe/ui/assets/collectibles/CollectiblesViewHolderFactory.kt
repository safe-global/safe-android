package io.gnosis.safe.ui.assets.collectibles

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import io.gnosis.safe.databinding.ItemCollectibleCollectibleBinding
import io.gnosis.safe.databinding.ItemCollectibleNftHeaderBinding
import io.gnosis.safe.ui.base.adapter.Adapter
import io.gnosis.safe.ui.base.adapter.BaseFactory
import io.gnosis.safe.ui.base.adapter.UnsupportedViewType

enum class CollectiblesViewType {
    NFT_HEADER,
    COLLECTIBLE_ITEM
}

class CollectiblesViewHolderFactory : BaseFactory<BaseCollectiblesViewHolder<CollectibleViewData>, CollectibleViewData>() {

    override fun newViewHolder(viewBinding: ViewBinding, viewType: Int): BaseCollectiblesViewHolder<CollectibleViewData> =
        when (viewType) {
            CollectiblesViewType.NFT_HEADER.ordinal -> NftHeaderViewHolder(viewBinding as ItemCollectibleNftHeaderBinding)
            CollectiblesViewType.COLLECTIBLE_ITEM.ordinal -> CollectibleItemViewHolder(viewBinding as ItemCollectibleCollectibleBinding)
            else -> throw UnsupportedViewType(javaClass.name)
        } as BaseCollectiblesViewHolder<CollectibleViewData>

    override fun layout(layoutInflater: LayoutInflater, parent: ViewGroup, viewType: Int): ViewBinding =
        when(viewType) {
            CollectiblesViewType.NFT_HEADER.ordinal -> ItemCollectibleNftHeaderBinding.inflate(layoutInflater, parent, false)
            CollectiblesViewType.COLLECTIBLE_ITEM.ordinal -> ItemCollectibleCollectibleBinding.inflate(layoutInflater, parent, false)
            else -> throw UnsupportedViewType(javaClass.name)
        }

    override fun viewTypeFor(item: CollectibleViewData): Int =
        when(item) {
            is CollectibleViewData.NftHeader -> CollectiblesViewType.NFT_HEADER
            is CollectibleViewData.CollectibleItem -> CollectiblesViewType.COLLECTIBLE_ITEM
        }.ordinal
}

abstract class BaseCollectiblesViewHolder<T : CollectibleViewData>(viewBinding: ViewBinding) : Adapter.ViewHolder<T>(viewBinding.root)

class NftHeaderViewHolder(private val viewBinding: ItemCollectibleNftHeaderBinding) :
    BaseCollectiblesViewHolder<CollectibleViewData.NftHeader>(viewBinding) {

    override fun bind(data: CollectibleViewData.NftHeader, payloads: List<Any>) {
        viewBinding.tokenName.text = data.tokenName
    }
}

class CollectibleItemViewHolder(private val viewBinding: ItemCollectibleCollectibleBinding) :
    BaseCollectiblesViewHolder<CollectibleViewData.CollectibleItem>(viewBinding) {

    override fun bind(data: CollectibleViewData.CollectibleItem, payloads: List<Any>) {
        viewBinding.name.text = data.collectible.name
    }
}
