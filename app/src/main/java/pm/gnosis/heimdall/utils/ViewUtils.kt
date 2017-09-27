package pm.gnosis.heimdall.utils

import android.widget.TextView
import com.jakewharton.rxbinding2.widget.editorActions
import io.reactivex.Observable


fun TextView.filterEditorActions(actionId: Int): Observable<Int> =
        this.editorActions(io.reactivex.functions.Predicate { it == actionId }).filter { it == actionId }