package io.gnosis.safe.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.view.LayoutInflater
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import io.gnosis.safe.R
import io.gnosis.safe.databinding.DialogRemoveBinding
import io.gnosis.safe.qrscanner.QRCodeScanActivity
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.utils.HttpCodes
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

fun handleQrCodeActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
    onQrCodeResult: (String) -> Unit,
    onCancelledResult: (() -> Unit)? = null
): Boolean {
    if (requestCode == QRCodeScanActivity.REQUEST_CODE) {
        if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra(QRCodeScanActivity.RESULT_EXTRA)) {
            onQrCodeResult(data.getStringExtra(QRCodeScanActivity.RESULT_EXTRA))
        } else if (resultCode == Activity.RESULT_CANCELED) {
            onCancelledResult?.invoke()
        }
        return true
    }
    return false
}

fun handleAddressBookResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
    onResult: (AddressBookEntry) -> Unit,
    onCancelled: (() -> Unit)? = null
): Boolean {
    // TODO uncomment when address book functionality is ready
//    if (requestCode == AddressBookActivity.REQUEST_CODE) {
//        if (resultCode == Activity.RESULT_OK) {
//            AddressBookActivity.parseResult(data)?.let { onResult(it) }
//        } else if (resultCode == Activity.RESULT_CANCELED) {
//            onCancelled?.invoke()
//        }
//        return true
//    }
    return false
}

fun pxToDp(px: Int): Int {
    return (px / Resources.getSystem().displayMetrics.density).toInt()
}

fun dpToPx(dp: Int): Int {
    return (dp * Resources.getSystem().displayMetrics.density).toInt()
}

fun Throwable.getErrorResForException(): Int =
    when {
        this is HttpException -> {
            this.let {
                when (this.code()) {
                    HttpCodes.FORBIDDEN, HttpCodes.UNAUTHORIZED -> R.string.error_not_authorized_for_action
                    HttpCodes.SERVER_ERROR, HttpCodes.BAD_REQUEST -> R.string.error_try_again
                    else -> R.string.error_try_again
                }
            }
        }
        this is SSLHandshakeException || this.cause is SSLHandshakeException -> {
            R.string.error_ssl_handshake
        }
        this is SocketTimeoutException -> {
            R.string.error_timeout
        }
        this is UnknownHostException || this is ConnectException -> {
            R.string.error_no_internet
        }
        else -> R.string.error_try_again
    }

fun showConfirmDialog(
    context: Context,
    @StringRes message: Int,
    @StringRes confirm: Int = R.string.safe_settings_dialog_remove,
    @ColorRes confirmColor: Int = R.color.tomato,
    confirmCallback: () -> Unit
) {
    val dialogBinding = DialogRemoveBinding.inflate(LayoutInflater.from(context), null, false)
    dialogBinding.message.setText(message)
    CustomAlertDialogBuilder.build(
        context = context,
        confirmCallback = { dialog ->
            confirmCallback()
            dialog.dismiss()
        },
        contentView = dialogBinding.root,
        confirmRes = confirm,
        cancelRes = R.string.safe_settings_dialog_cancel,
        confirmColor = confirmColor,
        cancelColor = R.color.safe_green
    ).show()
}
