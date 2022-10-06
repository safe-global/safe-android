package io.gnosis.safe.ui.assets.collectibles

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.navigation.Navigation
import androidx.viewbinding.ViewBinding
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemCollectibleCollectibleBinding
import io.gnosis.safe.databinding.ItemCollectibleNftHeaderBinding
import io.gnosis.safe.ui.assets.AssetsFragmentDirections
import io.gnosis.safe.ui.base.adapter.Adapter
import io.gnosis.safe.ui.base.adapter.BaseFactory
import io.gnosis.safe.ui.base.adapter.UnsupportedViewType
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddressString

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
        when (viewType) {
            CollectiblesViewType.NFT_HEADER.ordinal -> ItemCollectibleNftHeaderBinding.inflate(layoutInflater, parent, false)
            CollectiblesViewType.COLLECTIBLE_ITEM.ordinal -> ItemCollectibleCollectibleBinding.inflate(layoutInflater, parent, false)
            else -> throw UnsupportedViewType(javaClass.name)
        }

    override fun viewTypeFor(item: CollectibleViewData): Int =
        when (item) {
            is CollectibleViewData.NftHeader -> CollectiblesViewType.NFT_HEADER
            is CollectibleViewData.CollectibleItem -> CollectiblesViewType.COLLECTIBLE_ITEM
        }.ordinal
}

abstract class BaseCollectiblesViewHolder<T : CollectibleViewData>(viewBinding: ViewBinding) : Adapter.ViewHolder<T>(viewBinding.root)

class NftHeaderViewHolder(private val viewBinding: ItemCollectibleNftHeaderBinding) :
    BaseCollectiblesViewHolder<CollectibleViewData.NftHeader>(viewBinding) {

    override fun bind(data: CollectibleViewData.NftHeader, payloads: List<Any>) {
        with(viewBinding) {
            if(data.tokenName.isNullOrBlank()) {
                tokenName.setText(R.string.collectibles_unknown)
            } else {
                tokenName.text = data.tokenName
            }
            separator.visible(!data.first)
            tokenLogo.loadNftImage(data.contractLogoUri)
        }
    }
}

class CollectibleItemViewHolder(private val viewBinding: ItemCollectibleCollectibleBinding) :
    BaseCollectiblesViewHolder<CollectibleViewData.CollectibleItem>(viewBinding), Target {

    override fun bind(data: CollectibleViewData.CollectibleItem, payloads: List<Any>) {
        with(viewBinding) {
            if(data.collectible.name.isNullOrBlank()) {
                name.setText(R.string.collectibles_unknown)
            } else {
                name.text = data.collectible.name
            }
            description.text = data.collectible.description
            logo.loadCollectibleImage(data.collectible.imageUri, this@CollectibleItemViewHolder)
            root.setOnClickListener {
                Navigation.findNavController(it).navigate(
                    AssetsFragmentDirections.actionAssetsFragmentToCollectiblesDetailsFragment(
                        data.chain,
                        data.collectible.address.asEthereumAddressString(),
                        data.collectible.name,
                        data.collectible.id,
                        data.collectible.description,
                        data.collectible.uri,
                        data.collectible.imageUri
                    )
                )
            }
        }
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        viewBinding.logo.setCollectiblePlaceholder()
    }

    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
        viewBinding.logo.setCollectiblePlaceholder()
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
        viewBinding.logo.setCollectibleBitmap(bitmap)
    }
}

fun ImageView.setCollectiblePlaceholder() {
    scaleType = ImageView.ScaleType.CENTER
    setImageResource(R.drawable.ic_collectible_placeholder)
    setBackgroundColor(context.getColorCompat(R.color.background_tertiary))
}

fun ImageView.setCollectibleBitmap(bitmap: Bitmap?) {
    scaleType = ImageView.ScaleType.CENTER_CROP
    setBackgroundColor(context.getColorCompat(R.color.white_two))
    setImageBitmap(bitmap)
}

fun ImageView.loadCollectibleImage(logo: String?, target: Target) {
    when {
        logo == null -> {
            setCollectiblePlaceholder()
        }
        !logo.isNullOrBlank() -> {
            Picasso.get()
                .load(logo)
                .into(target)
        }
        else -> {
            setCollectiblePlaceholder()
        }
    }
}

fun ImageView.loadNftImage(logo: String?) {
    when {
        logo == null -> {
            setImageResource(R.drawable.ic_nft_placeholder)
        }
        !logo.isNullOrBlank() -> {
            Picasso.get()
                .load(logo)
                .placeholder(R.drawable.ic_nft_placeholder)
                .error(R.drawable.ic_nft_placeholder)
                .into(this)
        }
        else -> {
            setImageResource(R.drawable.ic_nft_placeholder)
        }
    }
}
