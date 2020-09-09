package io.gnosis.safe.ui.assets.collectibles

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.setPadding
import androidx.viewbinding.ViewBinding
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.squareup.picasso.PicassoProvider
import com.squareup.picasso.Target
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemCollectibleCollectibleBinding
import io.gnosis.safe.databinding.ItemCollectibleNftHeaderBinding
import io.gnosis.safe.ui.base.adapter.Adapter
import io.gnosis.safe.ui.base.adapter.BaseFactory
import io.gnosis.safe.ui.base.adapter.UnsupportedViewType
import io.gnosis.safe.utils.CircleTransformation
import io.gnosis.safe.utils.dpToPx
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.visible
import java.lang.Exception

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
            tokenName.text = data.tokenName
            separator.visible(!data.first)
            //tokenLogo
        }
    }
}

class CollectibleItemViewHolder(private val viewBinding: ItemCollectibleCollectibleBinding) :
    BaseCollectiblesViewHolder<CollectibleViewData.CollectibleItem>(viewBinding), Target {

    override fun bind(data: CollectibleViewData.CollectibleItem, payloads: List<Any>) {
        with(viewBinding) {
            name.text = data.collectible.name
            description.text = data.collectible.description
            logo.loadNftImage(data.collectible.imageUri, this@CollectibleItemViewHolder)
        }
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        viewBinding.logo.setNftPlaceholder()
    }

    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
        viewBinding.logo.setNftPlaceholder()
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
        viewBinding.logo.setNftBitmap(bitmap)
    }
}

fun ImageView.setNftPlaceholder() {
    scaleType = ImageView.ScaleType.CENTER
    setImageResource(R.drawable.ic_collectible_placeholder)
    setBackgroundColor(context.getColorCompat(R.color.whitesmoke_two))
}

fun ImageView.setNftBitmap(bitmap: Bitmap?) {
    scaleType = ImageView.ScaleType.FIT_CENTER
    background = null
    setImageBitmap(bitmap)
}

fun ImageView.loadNftImage(logo: String?, target: Target) {
    when {
        logo == null -> {
            setNftPlaceholder()
        }
        !logo.isNullOrBlank() -> {
            Picasso.get()
                .load(logo)
                .into(target)
        }
        else -> {
            setNftPlaceholder()
        }
    }
}
