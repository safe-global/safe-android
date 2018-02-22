package pm.gnosis.blockies

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.widget.ImageView
import java.math.BigInteger

open class BlockiesImageView(context: Context, attributeSet: AttributeSet) : ImageView(context, attributeSet) {
    private val canvasPaint = Paint().apply { style = Paint.Style.FILL }
    private var dimen = 0.0f
    private var offsetX = 0.0f
    private var offsetY = 0.0f
    private val path = Path()

    private var blockies: Blockies? = null

    fun setAddress(address: BigInteger) {
        blockies = Blockies.fromAddress(address)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        blockies?.let { drawBlockies(canvas, it) }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        dimen = Math.min(measuredWidth, measuredHeight).toFloat()
        offsetX = measuredWidth - dimen
        offsetY = measuredHeight - dimen
        path.reset()
        path.addCircle(offsetX + (dimen / 2), offsetY + (dimen / 2), dimen / 2, Path.Direction.CCW)
        path.close()
    }

    private fun drawBlockies(canvas: Canvas, blockies: Blockies) {
        canvas.save()
        canvas.clipPath(path)
        canvasPaint.color = blockies.backgroundColor
        canvas.drawRect(
                offsetX, offsetY, offsetX + dimen, offsetY + dimen,
                canvasPaint
        )

        val scale = dimen / Blockies.SIZE
        val main = blockies.primaryColor
        val sColor = blockies.spotColor

        for (i in blockies.data.indices) {
            val col = i % Blockies.SIZE
            val row = i / Blockies.SIZE

            canvasPaint.color = if (blockies.data[i] == 1.0) main else sColor

            if (blockies.data[i] > 0.0) {
                canvas.drawRect(offsetX + (col * scale), offsetY + (row * scale), offsetX + (col * scale + scale), offsetY + (row * scale + scale), canvasPaint)
            }
        }

        canvas.restore()
    }
}
