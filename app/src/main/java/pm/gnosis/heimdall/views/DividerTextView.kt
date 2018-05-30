package pm.gnosis.heimdall.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.support.v4.view.ViewCompat
import android.support.v7.widget.AppCompatTextView
import android.util.AttributeSet
import android.view.Gravity
import pm.gnosis.heimdall.R
import pm.gnosis.svalinn.common.utils.getColorCompat

class DividerTextView : AppCompatTextView {

    private var textBoundsRect: Rect? = Rect()
    private var dividerPaint: Paint? = Paint()
    private var dividerOffset: Float = context.resources.getDimension(R.dimen.divider_offset)
    private var dividerPadding: Float = context.resources.getDimension(R.dimen.divider_padding)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        dividerPaint?.color = context.getColorCompat(R.color.divider)
        dividerPaint?.strokeWidth = context.resources.getDimension(R.dimen.divider_width)
        updateTextBounds()
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        updateTextBounds()
    }

    private fun updateTextBounds() {
        textBoundsRect?.let { paint.getTextBounds(text.toString(), 0, text.length, it) }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        val isLtr = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_LTR
        when (gravity) {
            Gravity.CENTER, Gravity.CENTER_HORIZONTAL -> drawCentered(canvas)
            Gravity.LEFT -> drawLeft(canvas)
            Gravity.RIGHT -> drawRight(canvas)
            Gravity.START -> if (isLtr) drawRight(canvas) else drawLeft(canvas)
            Gravity.END -> if (isLtr) drawLeft(canvas) else drawRight(canvas)
        }
    }

    private fun drawCentered(canvas: Canvas) {
        textBoundsRect?.let {
            val centerY = canvas.height / 2f + dividerOffset
            val centerX = canvas.width / 2f
            val centerOffset = it.width() / 2f + dividerPadding
            canvas.drawLine(0f, centerY, centerX - centerOffset, centerY, dividerPaint)
            canvas.drawLine(centerX + centerOffset, centerY, canvas.width.toFloat(), centerY, dividerPaint)
        }
    }

    private fun drawLeft(canvas: Canvas) {
        textBoundsRect?.let {
            val centerY = canvas.height / 2f + dividerOffset
            val offset = it.width() + dividerPadding
            canvas.drawLine(offset, centerY, canvas.width.toFloat(), centerY, dividerPaint)
        }
    }

    private fun drawRight(canvas: Canvas) {
        textBoundsRect?.let {
            val centerY = canvas.height / 2f + dividerOffset
            val offset = it.width() + dividerPadding
            canvas.drawLine(0f, centerY, canvas.width.toFloat() - offset, centerY, dividerPaint)
        }
    }
}
