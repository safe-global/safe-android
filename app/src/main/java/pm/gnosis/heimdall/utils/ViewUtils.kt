package pm.gnosis.heimdall.utils

import android.support.v4.view.ViewCompat
import android.view.View
import android.widget.TextView
import com.jakewharton.rxbinding2.widget.editorActions
import com.squareup.phrase.Phrase
import io.reactivex.Observable

fun View.disableAccessibility() {
    ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
}

// Gets the initial letters of the first and last words of a string
fun String.initials(): String {
    val words = split(" ")
    return ((words.firstOrNull()?.firstOrNull()?.toString() ?: "") +
            (if (words.size > 1) words.lastOrNull()?.firstOrNull()?.toString() ?: "" else ""))
}

fun TextView.setFormattedString(res: Int, vararg params: Pair<String, String>) {
    text = Phrase.from(this, res).apply {
        params.forEach { (key, value) ->
            put(key, value)
        }
    }.format()
}
