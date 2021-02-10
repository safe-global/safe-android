package io.gnosis.safe.ui.settings.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import com.google.android.material.imageview.ShapeableImageView
import pm.gnosis.blockies.Blockies
import pm.gnosis.blockies.BlockiesPainter
import pm.gnosis.model.Solidity

open class KnownAddressLogoView(context: Context, attributeSet: AttributeSet?) : ShapeableImageView(context, attributeSet) {

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
}
