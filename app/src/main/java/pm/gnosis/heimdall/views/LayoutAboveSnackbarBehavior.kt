package pm.gnosis.heimdall.views

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.util.AttributeSet
import android.view.View

open class LayoutAboveSnackbarBehavior<V : View>(context: Context, attrs: AttributeSet) : CoordinatorLayout.Behavior<V>() {

    override fun layoutDependsOn(parent: CoordinatorLayout?, child: V?, dependency: View?): Boolean {
        return dependency is Snackbar.SnackbarLayout
    }

    override fun onDependentViewRemoved(parent: CoordinatorLayout?, child: V?, dependency: View?) {
        child?.animate()?.translationY(0f)?.duration = 100
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout?, child: V?, dependency: View?): Boolean {
        dependency?.let {
            val translationY = Math.min(0f, dependency.translationY - dependency.height)
            child?.animate()?.cancel()
            child?.translationY = translationY
        }
        return true
    }
}
