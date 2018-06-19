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


object CustomAlertDialogBuilder {
    fun build(
        context: Context,
        title: CharSequence,
        contentView: View,
        @StringRes confirmRes: Int,
        confirmCallback: (DialogInterface, Int) -> Unit,
        @StringRes cancelRes: Int = android.R.string.cancel,
        cancelCallback: (DialogInterface, Int) -> Unit = { dialog, _ -> dialog.dismiss() },
        @ColorRes confirmColor: Int = R.color.azure,
        @ColorRes cancelColor: Int = R.color.azure
    ): AlertDialog =
        AlertDialog.Builder(context).apply {
            setPositiveButton(confirmRes, confirmCallback)
            setNegativeButton(cancelRes, cancelCallback)
            setView(contentView)
            setCustomTitle(LayoutInflater.from(context).inflate(R.layout.layout_alert_dialog_title, null).apply {
                layout_alert_dialog_title_text.text = title
            })
        }
            .create()
            .apply {
                setOnShowListener {
                    getButton(Dialog.BUTTON_POSITIVE).setTextColor(context.getColorCompat(confirmColor))
                    getButton(Dialog.BUTTON_NEGATIVE).setTextColor(context.getColorCompat(cancelColor))
                }
            }
}
