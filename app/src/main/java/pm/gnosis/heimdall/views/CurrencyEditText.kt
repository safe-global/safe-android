package pm.gnosis.heimdall.views

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.support.v7.widget.AppCompatEditText
import android.util.AttributeSet

class CurrencyEditText : AppCompatEditText {

    private var currencyDrawable: CurrencyDrawable? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        currencyDrawable = CurrencyDrawable()
        setCompoundDrawablesWithIntrinsicBounds(null, null, currencyDrawable, null)
    }

    override fun setTypeface(typeface: Typeface) {
        super.setTypeface(typeface)
        currencyDrawable?.textPaint?.typeface = typeface
        postInvalidate()
    }

    fun setCurrencySymbol(symbol: String?) {
        currencyDrawable?.symbol = symbol
        setCompoundDrawablesWithIntrinsicBounds(null, null, currencyDrawable, null)
    }

    private inner class CurrencyDrawable : Drawable() {

        val textPaint = Paint()

        var symbol: String? = null
            set(value) {
                field = value
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                invalidateSelf()
            }

        init {
            textPaint.color = currentHintTextColor
            textPaint.textSize = textSize
            textPaint.typeface = typeface
        }

        override fun draw(canvas: Canvas?) {
            val baseline = getLineBounds(0, null)
            symbol?.let { canvas?.drawText(it, 0f, (baseline + canvas.clipBounds.top).toFloat(), textPaint) }
        }

        override fun setAlpha(alpha: Int) {
        }

        override fun getOpacity() = PixelFormat.TRANSLUCENT

        override fun setColorFilter(colorFilter: ColorFilter?) {
        }

        override fun getIntrinsicHeight(): Int {
            return textPaint.textSize.toInt()
        }

        override fun getIntrinsicWidth(): Int {
            return symbol?.let { textPaint.measureText(it).toInt() } ?: 0
        }
    }
}
