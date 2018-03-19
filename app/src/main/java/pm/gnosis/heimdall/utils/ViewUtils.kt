package pm.gnosis.heimdall.utils

import android.graphics.PorterDuff
import android.os.Build
import android.support.annotation.ColorRes
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.view.ViewCompat
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.squareup.phrase.Phrase
import pm.gnosis.svalinn.common.utils.getColorCompat

fun View.disableAccessibility() {
    ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
}

fun TextView.setFormattedText(res: Int, vararg params: Pair<String, String>) {
    text = Phrase.from(this, res).apply {
        params.forEach { (key, value) ->
            put(key, value)
        }
    }.format()
}

fun ImageView.setColorFilterCompat(@ColorRes color: Int) = setColorFilter(context.getColorCompat(color))

fun ProgressBar.tintCompat(@ColorRes color: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        val drawableProgress = DrawableCompat.wrap(indeterminateDrawable)
        DrawableCompat.setTint(drawableProgress, context.getColorCompat(color))
        indeterminateDrawable = DrawableCompat.unwrap(drawableProgress)
    } else {
        indeterminateDrawable.setColorFilter(context.getColorCompat(color), PorterDuff.Mode.SRC_IN)
    }
}
