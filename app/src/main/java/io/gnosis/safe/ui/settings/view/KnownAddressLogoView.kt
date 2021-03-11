package io.gnosis.safe.ui.settings.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import io.gnosis.safe.R
import pm.gnosis.blockies.Blockies
import pm.gnosis.blockies.BlockiesPainter
import pm.gnosis.model.Solidity


open class KnownAddressLogoView(context: Context, attributeSet: AttributeSet?) : ShapeableImageView(context, attributeSet), Target {

    private var blockies: Blockies? = null
    private var painter: BlockiesPainter = BlockiesPainter()

    fun setAddress(address: Solidity.Address?) {
        blockies = address?.let { Blockies.fromAddress(it) }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        blockies?.let { drawBlockies(canvas, it) }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        painter.setDimensions(measuredWidth.toFloat(), measuredHeight.toFloat())
    }

    private fun drawBlockies(canvas: Canvas, blockies: Blockies) {
        painter.draw(canvas, blockies)
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
        setAddress(null)
        setImageBitmap(bitmap)
        background = ContextCompat.getDrawable(context, R.drawable.circle)
    }

    fun loadKnownAddressLogo(logoUri: String?, address: Solidity.Address, target: Target = this) {
        setAddress(address)
        when {
            !logoUri.isNullOrBlank() -> {
                Picasso.get()
                    .load(logoUri)
                    .into(target)
            }
        }
    }

    fun loadKnownSafeAppLogo(logoUri: String?, defaultResId: Int = R.drawable.ic_code_16dp, target: Target = this) {
        setAddress(null)
        setImageResource(defaultResId)
        when {
            !logoUri.isNullOrBlank() -> {
                Picasso.get()
                    .load(logoUri)
                    .into(target)
            }
        }
    }
}
