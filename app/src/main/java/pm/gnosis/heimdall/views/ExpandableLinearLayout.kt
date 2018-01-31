package pm.gnosis.heimdall.views

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout

class ExpandableLinearLayout(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {
    private var currentValueAnimator: ValueAnimator? = null
    var duration = 250L

    fun show() {
        if (height == 0) doAction(show)
    }

    fun hide() {
        if (height != 0) doAction(hide)
    }

    private val show: () -> ValueAnimator = {
        measure()
        ValueAnimator.ofInt(0, measuredHeight)
    }

    private val hide: () -> ValueAnimator = {
        measure()
        ValueAnimator.ofInt(measuredHeight, 0)
    }

    private fun measure() {
        measure(View.MeasureSpec.makeMeasureSpec((parent as View).width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec((parent as View).height, View.MeasureSpec.AT_MOST))

    }

    private fun doAction(action: () -> ValueAnimator) {
        if (currentValueAnimator != null && currentValueAnimator?.isRunning == true) return

        val valueAnimator = action()
        valueAnimator.addUpdateListener {
            layoutParams = layoutParams.apply { height = valueAnimator.animatedValue as Int }
        }
        valueAnimator.duration = duration
        valueAnimator.start()
        this.currentValueAnimator = valueAnimator
    }
}
