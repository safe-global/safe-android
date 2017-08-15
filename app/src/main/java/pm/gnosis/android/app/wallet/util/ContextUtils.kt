package pm.gnosis.android.app.wallet.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.View
import android.widget.Toast
import pm.gnosis.android.app.wallet.util.zxing.ZxingIntentIntegrator


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

fun Activity.scanQrCode() = ZxingIntentIntegrator(this).initiateScan(ZxingIntentIntegrator.QR_CODE_TYPES)
fun Fragment.scanQrCode() = ZxingIntentIntegrator(this).initiateScan(ZxingIntentIntegrator.QR_CODE_TYPES)
