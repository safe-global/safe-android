package pm.gnosis.heimdall.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import pm.gnosis.heimdall.R

class ReceiptDivider : View {

    private var dividerPaint: Paint? = Paint()
    private var dashLength: Float = context.resources.getDimension(R.dimen.receipt_divider_length)
    private var dashGap: Float = context.resources.getDimension(R.dimen.receipt_divider_gap)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        dividerPaint?.color = ContextCompat.getColor(context, R.color.divider)
        dividerPaint?.strokeWidth = context.resources.getDimension(R.dimen.receipt_divider_width)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        val dashCount = (measuredWidth - dashGap - paddingLeft - paddingRight) / (dashLength + dashGap)
        (0 until dashCount.toInt()).forEach {
            val start = paddingStart + (2 * it + 1) * dashGap
            canvas.drawLine(start, height / 2f, start + dashLength, height / 2f, dividerPaint)
        }
    }
}
