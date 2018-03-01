package pm.gnosis.heimdall.views

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout

class ExpandableLinearLayout(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {
    private var currentValueAnimator: ValueAnimator? = null
    var animationDuration = 250L
    private var isShowing = false

    fun show() {
        isShowing = true
        doAction(show)
    }

    fun hide() {
        isShowing = false
        doAction(hide)
    }

    fun toggle() = if (isShowing) hide() else show()

    private val show: () -> ValueAnimator = {
        measure()
        ValueAnimator.ofInt(height, measuredHeight)
    }

    private val hide: () -> ValueAnimator = { ValueAnimator.ofInt(height, 0) }

    private fun measure() = measure(
        View.MeasureSpec.makeMeasureSpec((parent as View).width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec((parent as View).height, View.MeasureSpec.AT_MOST)
    )

    private fun doAction(action: () -> ValueAnimator) {
        currentValueAnimator?.cancel()
        currentValueAnimator = action().apply {
            addUpdateListener {
                layoutParams = layoutParams.apply { height = animatedValue as Int }
            }
            duration = animationDuration
            start()
        }
    }
}
