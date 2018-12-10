package pm.gnosis.heimdall.utils

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.support.annotation.ColorRes
import android.support.annotation.StringRes
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import kotlinx.android.synthetic.main.layout_alert_dialog_title.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.visible


object CustomAlertDialogBuilder {
    fun build(
        context: Context,
        title: CharSequence,
        contentView: View,
        @StringRes confirmRes: Int,
        confirmCallback: ((DialogInterface) -> Unit)?,
        @StringRes cancelRes: Int = android.R.string.cancel,
        cancelCallback: ((DialogInterface) -> Unit)? = { dialog -> dialog.dismiss() },
        @ColorRes confirmColor: Int = R.color.azure,
        @ColorRes cancelColor: Int = R.color.azure
    ): AlertDialog =
        AlertDialog.Builder(context).apply {
            if (confirmRes != 0) setPositiveButton(confirmRes, null)
            if (cancelRes != 0) setNegativeButton(cancelRes, null)
            setView(contentView)
            setCustomTitle(LayoutInflater.from(context).inflate(R.layout.layout_alert_dialog_title, null).apply {
                layout_alert_dialog_title_text.text = title
            })
        }
            .create()
            .apply {
                setOnShowListener { dialog ->
                    getButton(Dialog.BUTTON_POSITIVE).apply {
                        if (confirmRes != 0) {
                            visible(true)
                            setTextColor(context.getColorCompat(confirmColor))
                            confirmCallback?.let { cb -> setOnClickListener { cb(dialog) } }
                        } else {
                            visible(false)
                        }
                    }
                    getButton(Dialog.BUTTON_NEGATIVE).apply {
                        if (cancelRes != 0) {
                            visible(true)
                            setTextColor(context.getColorCompat(cancelColor))
                            cancelCallback?.let { cb -> setOnClickListener { cb(dialog) } }
                        } else {
                            visible(false)
                        }
                    }
                }
            }
}
