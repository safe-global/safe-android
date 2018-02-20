package pm.gnosis.heimdall.utils

import android.support.annotation.ColorRes
import android.support.v4.view.ViewCompat
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.squareup.phrase.Phrase
import pm.gnosis.heimdall.common.utils.getColorCompat

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
