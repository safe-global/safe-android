package io.gnosis.safe.utils

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.ButtonBarLayout
import io.gnosis.safe.R
import io.gnosis.safe.databinding.LayoutAlertDialogTitleBinding
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.visible

object CustomAlertDialogBuilder {
    fun build(
        context: Context,
        title: CharSequence? = null,
        contentView: View,
        @StringRes confirmRes: Int,
        confirmCallback: ((DialogInterface) -> Unit)?,
        @StringRes cancelRes: Int = android.R.string.cancel,
        cancelCallback: ((DialogInterface) -> Unit)? = { dialog -> dialog.dismiss() },
        @ColorRes confirmColor: Int = R.color.primary,
        @ColorRes cancelColor: Int = R.color.primary,
        @ColorRes background: Int = R.color.background_secondary,
        dismissCallback: DialogInterface.OnDismissListener? = null
    ): AlertDialog {
        val binding = LayoutAlertDialogTitleBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(context).apply {
            if (confirmRes != 0) setPositiveButton(confirmRes, null)
            if (cancelRes != 0) setNegativeButton(cancelRes, null)
            if (dismissCallback != null) setOnDismissListener(dismissCallback)
        }
            .setView(contentView)
            .setCustomTitle(binding.root.apply {
                if (title == null) {
                    binding.layoutAlertDialogTitleText.visible(false)
                } else {
                    binding.layoutAlertDialogTitleText.visible(true)
                    binding.layoutAlertDialogTitleText.text = title
                }
            })
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
                    (getButton(Dialog.BUTTON_POSITIVE).parent as ButtonBarLayout).setBackgroundColor(context.getColorCompat(background))
                }
            }
    }
}

object InfoTipDialogBuilder {
    fun build(
        context: Context,
        @LayoutRes contentRes: Int,
        @StringRes confirmRes: Int,
        @ColorRes confirmColor: Int = R.color.primary
    ): AlertDialog {
        val alertContent = LayoutInflater.from(context).inflate(contentRes, null)
        return AlertDialog.Builder(context)
            .setView(alertContent)
            .setPositiveButton(confirmRes) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .apply {
                setOnShowListener { dialog ->
                    getButton(Dialog.BUTTON_POSITIVE).apply {
                        setTextColor(context.getColorCompat(confirmColor))
                    }
                }
            }
    }
}
