package pm.gnosis.heimdall.common.utils

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Context.VIBRATOR_SERVICE
import android.net.Uri
import android.os.Vibrator
import android.provider.Browser
import android.support.annotation.PluralsRes
import android.support.annotation.RequiresPermission
import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import timber.log.Timber


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

fun Context.openUrl(url: String) {
    val uri = Uri.parse(url)
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.putExtra(Browser.EXTRA_APPLICATION_ID, packageName)
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Timber.e(e)
    }

}

fun Activity.startActivity(i: Intent, clearStack: Boolean = false) {
    if (clearStack) {
        i.clearStack()
    }
    startActivity(i)
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
}

fun Intent.clearStack(): Intent {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    return this
}

fun Intent.noHistory(): Intent {
    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    return this
}

fun View.showKeyboardForView() {
    requestFocus()
    (context.getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun Activity.hideSoftKeyboard() {
    val view = this.currentFocus
    if (view != null) {
        (getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

@RequiresPermission(Manifest.permission.VIBRATE)
fun Context.vibrate(milliseconds: Long) =
        (getSystemService(VIBRATOR_SERVICE) as? Vibrator)?.vibrate(milliseconds)
