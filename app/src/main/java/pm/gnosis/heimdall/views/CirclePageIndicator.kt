package pm.gnosis.heimdall.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.viewpager.widget.ViewPager
import pm.gnosis.heimdall.R
import pm.gnosis.svalinn.common.utils.getColorCompat


class CirclePageIndicator @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), ViewPager.OnPageChangeListener {

    private var viewPager: ViewPager? = null

    private var currentPosition: Int = 0
    private var currentPositionOffset: Float = 0f
    private var scrollSate: Int = ViewPager.SCROLL_STATE_IDLE

    var radius: Float = context.resources.getDimension(R.dimen.indicator_size) / 2
        set(value) {
            field = value
            invalidate()
        }

    var spacing: Float = radius * 2
        set(value) {
            field = value
            invalidate()
        }

    private val activePaint = Paint().apply {
        style = Paint.Style.FILL
        color = context.getColorCompat(R.color.safe_green)
    }
    private val inactivePaint = Paint(activePaint).apply {
        color = context.getColorCompat(R.color.medium_grey)
    }

    fun setActiveColor(@ColorInt color: Int) {
        activePaint.color = color
    }

    fun setInactiveColor(@ColorInt color: Int) {
        inactivePaint.color = color
    }

    fun setViewPager(pager: ViewPager) {
        viewPager?.removeOnPageChangeListener(this)
        viewPager = pager
        viewPager?.addOnPageChangeListener(this)
        currentPosition = viewPager?.currentItem ?: 0
        currentPositionOffset = 0f
    }

    override fun onPageScrollStateChanged(state: Int) {
        scrollSate = state
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        currentPosition = position
        currentPositionOffset = positionOffset
        invalidate()
    }

    override fun onPageSelected(position: Int) {
        if (scrollSate == ViewPager.SCROLL_STATE_IDLE) {
            currentPosition = position
            currentPositionOffset = 0f
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val count = viewPager?.adapter?.count ?: 0
        if (count == 0 || currentPosition >= count) {
            return
        }

        val itemSpace = radius * 2 + spacing
        val yOffset = paddingTop + radius + (height - paddingTop - paddingBottom) / 2f - radius / 2f
        val xOffset = paddingLeft + (width - paddingLeft - paddingRight) / 2f - (count - 1) * (radius + (spacing / 2f))

        (0 until count).forEach {
            canvas.drawCircle(xOffset + it * itemSpace, yOffset, radius, inactivePaint)
        }

        val cx = currentPosition * itemSpace
        canvas.drawCircle(xOffset + cx, yOffset, radius, activePaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec))
    }

    private fun measureWidth(measureSpec: Int): Int {
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)

        if (specMode == MeasureSpec.EXACTLY || viewPager?.adapter == null) {
            return specSize
        }
        val count = viewPager?.adapter?.count ?: return 0
        val result = (paddingLeft + paddingRight + count * 2 * radius + (count - 1) * radius + 1).toInt()
        if (specMode != MeasureSpec.AT_MOST) {
            return result
        }
        return Math.min(result, specSize)
    }

    private fun measureHeight(measureSpec: Int): Int {
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)

        if (specMode == MeasureSpec.EXACTLY) {
            return specSize
        }
        val result = (2 * radius + paddingTop + paddingBottom + 1).toInt()
        if (specMode != MeasureSpec.AT_MOST) {
            return result
        }
        return Math.min(result, specSize)
    }

}