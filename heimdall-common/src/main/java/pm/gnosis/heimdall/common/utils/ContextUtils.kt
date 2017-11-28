package pm.gnosis.heimdall.common.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.support.annotation.PluralsRes
import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast


fun Context.toast(text: CharSequence, duration: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, text, duration).show()
}

fun Context.toast(@StringRes text: Int, duration: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, text, duration).show()
}

fun snackbar(view: View, text: CharSequence, duration: Int = Snackbar.LENGTH_LONG, action: Pair<String, (View) -> Unit>? = null) {
    val snackbar = Snackbar.make(view, text, duration)
    action?.let { snackbar.setAction(it.first, it.second) }
    snackbar.show()
}

fun snackbar(view: View, @StringRes textId: Int, duration: Int = Snackbar.LENGTH_LONG, action: Pair<Int, (View) -> Unit>? = null) {
    val snackbar = Snackbar.make(view, textId, duration)
    action?.let { snackbar.setAction(it.first, it.second) }
    snackbar.show()
}

fun Context.getSimplePlural(@PluralsRes stringId: Int, quantity: Long): String =
        resources.getQuantityString(stringId, quantity.toInt(), quantity)

fun Context.copyToClipboard(label: String, text: String, onCopy: (String) -> Unit = {}) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.primaryClip = ClipData.newPlainText(label, text)
    onCopy(text)
}

fun Context.shareExternalText(text: String, dialogTitle: String = "") {
    val sendIntent = Intent()
    sendIntent.action = Intent.ACTION_SEND
    sendIntent.putExtra(Intent.EXTRA_TEXT, text)
    sendIntent.type = "text/plain"
    startActivity(Intent.createChooser(sendIntent, dialogTitle))
}

fun Context.shareExternalText(text: String, @StringRes stringId: Int) =
        shareExternalText(text, getString(stringId))

fun Activity.startActivity(i: Intent, noHistory: Boolean = false) {
    if (noHistory) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    startActivity(i)
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
}

fun Activity.hideSoftKeyboard() {
    val view = this.currentFocus
    if (view != null) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
