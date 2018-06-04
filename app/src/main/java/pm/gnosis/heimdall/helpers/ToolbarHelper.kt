package pm.gnosis.heimdall.helpers

import android.support.v4.widget.NestedScrollView
import android.view.View
import com.jakewharton.rxbinding2.support.v4.widget.scrollChangeEvents
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolbarHelper @Inject constructor() {
    fun setupShadow(
        toolbarShadow: View,
        scrollView: NestedScrollView
    ): List<Disposable> {
        toolbarShadow.setRelativeScrollAlpha(scrollView.scrollY)
        return listOf(
            scrollView.scrollChangeEvents()
                .subscribeBy(
                    onNext = {
                        toolbarShadow.setRelativeScrollAlpha(it.scrollY())
                    }
                )
        )
    }

    private fun View.setRelativeScrollAlpha(scrollY: Int, shadowSize: Float = height.toFloat()) {
        val scrollShadowDiff = Math.max(0f, shadowSize - scrollY)
        alpha = if (shadowSize > 0) 1 - (scrollShadowDiff / shadowSize) else 0f
    }
}
