package io.gnosis.safe.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.view.LayoutInflater
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.navigation.NavController
import io.gnosis.safe.R
import io.gnosis.safe.databinding.DialogRemoveBinding
import io.gnosis.safe.qrscanner.QRCodeScanActivity
import pm.gnosis.models.AddressBookEntry

fun handleQrCodeActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
    onQrCodeResult: (String) -> Unit,
    onCancelledResult: (() -> Unit)? = null
): Boolean {
    if (requestCode == QRCodeScanActivity.REQUEST_CODE) {
        if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra(QRCodeScanActivity.RESULT_EXTRA)) {
            onQrCodeResult(data.getStringExtra(QRCodeScanActivity.RESULT_EXTRA)!!)
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

fun showConfirmDialog(
    context: Context,
    title: Int? = null,
    @StringRes message: Int,
    @StringRes confirm: Int = R.string.safe_settings_dialog_remove,
    @ColorRes confirmColor: Int = R.color.error,
    dismissCallback: DialogInterface.OnDismissListener = DialogInterface.OnDismissListener { },
    confirmCallback: () -> Unit
): Dialog {
    val dialogBinding = DialogRemoveBinding.inflate(LayoutInflater.from(context), null, false)
    dialogBinding.message.setText(message)
    return CustomAlertDialogBuilder.build(
        context = context,
        confirmCallback = { dialog ->
            confirmCallback()
            dialog.dismiss()
        },
        dismissCallback = dismissCallback,
        contentView = dialogBinding.root,
        confirmRes = confirm,
        cancelRes = R.string.safe_settings_dialog_cancel,
        confirmColor = confirmColor,
        cancelColor = R.color.primary,
        title = if (title == null) null else context.resources.getString(title)
    ).apply {
        show()
    }
}

fun <T>NavController.setToCurrent(key: String, value: T) {
    currentBackStackEntry?.savedStateHandle?.set(key, value)
}

fun <T>NavController.setToPrevious(key: String, value: T) {
    previousBackStackEntry?.savedStateHandle?.set(key, value)
}

fun <T>NavController.getFromCurrent(key: String): T? {
    return currentBackStackEntry?.savedStateHandle?.get<T>(key)
}
