package pm.gnosis.heimdall.utils

import android.support.v4.view.ViewCompat
import android.view.View
import android.widget.TextView
import com.jakewharton.rxbinding2.widget.editorActions
import io.reactivex.Observable

fun TextView.filterEditorActions(actionId: Int): Observable<Int> =
        this.editorActions(io.reactivex.functions.Predicate { it == actionId }).filter { it == actionId }

fun View.disableAccessibility() {
    ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
}

// Gets the initial letters of the first and last words of a string
fun String.initials(): String {
    val words = split(" ")
    return ((words.firstOrNull()?.firstOrNull()?.toString() ?: "") +
            (if (words.size > 1) words.lastOrNull()?.firstOrNull()?.toString() ?: "" else ""))
}
