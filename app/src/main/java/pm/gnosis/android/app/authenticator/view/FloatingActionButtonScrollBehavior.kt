package pm.gnosis.android.app.authenticator.view

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.View
import com.github.clans.fab.FloatingActionMenu
import pm.gnosis.android.app.authenticator.util.sameSign


class FloatingActionButtonScrollBehavior(context: Context, attrs: AttributeSet) : CoordinatorLayout.Behavior<FloatingActionMenu>() {
    private var acumulator = 0
    private var threshold = 0

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: FloatingActionMenu, directTargetChild: View, target: View, axes: Int, type: Int): Boolean {
        threshold = (if (child.childCount > 0) child.getChildAt(0).height else child.height) / 2
        return true
    }

    override fun onNestedScroll(coordinatorLayout: CoordinatorLayout, child: FloatingActionMenu, target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type)

        if (!sameSign(acumulator, dyConsumed)) { //scroll direction change
            acumulator = 0
        }
        acumulator += dyConsumed

        if (acumulator > threshold && !child.isMenuButtonHidden) {
            child.hideMenuButton(true)
        } else if (acumulator < -threshold && child.isMenuButtonHidden) {
            child.showMenuButton(true)
        }
    }

    override fun onStopNestedScroll(coordinatorLayout: CoordinatorLayout, child: FloatingActionMenu, target: View, type: Int) {
        super.onStopNestedScroll(coordinatorLayout, child, target, type)
        acumulator = 0
    }
}
