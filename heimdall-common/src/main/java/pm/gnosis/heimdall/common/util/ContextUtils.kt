package pm.gnosis.heimdall.common.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import kotlin.reflect.KClass


fun Context.toast(text: CharSequence, duration: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, text, duration).show()
}

fun snackbar(view: View, text: CharSequence, duration: Int = Snackbar.LENGTH_LONG) {
    Snackbar.make(view, text, duration).show()
}

fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.primaryClip = ClipData.newPlainText(label, text)
}

fun Context.shareExternalText(text: String, dialogTitle: String = "") {
    val sendIntent = Intent()
    sendIntent.action = Intent.ACTION_SEND
    sendIntent.putExtra(Intent.EXTRA_TEXT, text)
    sendIntent.type = "text/plain"
    startActivity(Intent.createChooser(sendIntent, dialogTitle))
}

fun <T : AppCompatActivity> Activity.startActivity(activity: KClass<T>, noHistory: Boolean = false) {
    val i = Intent(this, activity.java)
    if (noHistory) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    startActivity(i)
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
}
