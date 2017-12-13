package pm.gnosis.heimdall.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.design.widget.Snackbar
import android.view.View
import android.widget.Toast
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.ZxingIntentIntegrator
import pm.gnosis.heimdall.common.utils.snackbar
import pm.gnosis.heimdall.common.utils.toast
import pm.gnosis.heimdall.ui.exceptions.LocalizedException

fun errorSnackbar(view: View, throwable: Throwable, duration: Int = Snackbar.LENGTH_LONG) {
    val message = (throwable as? LocalizedException)?.localizedMessage() ?: view.context.getString(R.string.error_try_again)
    snackbar(view, message, duration)
}

fun Context.errorToast(throwable: Throwable, duration: Int = Toast.LENGTH_LONG) {
    val message = (throwable as? LocalizedException)?.localizedMessage() ?: getString(R.string.error_try_again)
    toast(message, duration)
}

fun handleQrCodeActivityResult(requestCode: Int, resultCode: Int, data: Intent?,
                               onQrCodeResult: (String) -> Unit, onCancelledResult: () -> Unit) {
    if (requestCode == ZxingIntentIntegrator.REQUEST_CODE) {
        if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA)) {
            onQrCodeResult(data.getStringExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA))
        } else if (resultCode == Activity.RESULT_CANCELED) {
            onCancelledResult()
        }
    }
}
