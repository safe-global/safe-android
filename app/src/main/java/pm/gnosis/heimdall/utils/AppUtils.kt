package pm.gnosis.heimdall.utils

import android.view.View
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.snackbar
import pm.gnosis.heimdall.ui.exceptions.LocalizedException


fun errorSnackbar(view: View, throwable: Throwable) {
    val message = (throwable as? LocalizedException)?.message ?: view.context.getString(R.string.error_try_again)
    snackbar(view, message)
}
