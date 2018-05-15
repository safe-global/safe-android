package pm.gnosis.heimdall.utils

import android.graphics.drawable.Drawable
import android.support.annotation.ColorRes
import android.support.v4.view.ViewCompat
import android.view.View
import android.widget.ImageView
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
// TODO: remove method after extracting colors
fun ImageView.setColorFilterCompat2(color: Int) = setColorFilter(color)

fun TextView.setSelectedCompoundDrawablesWithIntrinsicBounds(
    left: Drawable? = null,
    top: Drawable? = null,
    right: Drawable? = null,
    bottom: Drawable? = null
) =
    setCompoundDrawablesWithIntrinsicBounds(left, top, right, bottom)
