package pm.gnosis.heimdall.utils

import android.widget.TextView
import com.jakewharton.rxbinding2.widget.editorActions
import io.reactivex.Observable

fun TextView.filterEditorActions(actionId: Int): Observable<Int> =
        this.editorActions(io.reactivex.functions.Predicate { it == actionId }).filter { it == actionId }

// Gets the initial letters of the first and last words of a string
fun String.initials(): String {
    val words = split(" ")
    return ((words.firstOrNull()?.firstOrNull()?.toString() ?: "") +
            (if (words.size > 1) split(" ").lastOrNull()?.firstOrNull()?.toString() ?: "" else ""))
}
