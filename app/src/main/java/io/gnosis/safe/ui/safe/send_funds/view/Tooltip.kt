package io.gnosis.safe.ui.safe.send_funds.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import io.gnosis.safe.R

class Tooltip(
    val context: Context,
    text: String
) : PopupWindow(context) {

    init {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        isOutsideTouchable = true
        isFocusable = false
        contentView = LayoutInflater.from(context).inflate(R.layout.popup_tooltip, null)
        contentView.findViewById<TextView>(R.id.tooltip_text).text = text
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        contentView.setOnClickListener {
            dismiss()
        }
    }
}
