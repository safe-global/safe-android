package io.gnosis.safe.helpers

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import io.gnosis.safe.R
import io.gnosis.safe.utils.formatEthAddressBold

class AddressTooltip(
    val context: Context,
    address: String
) : PopupWindow(context) {

    init {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        isOutsideTouchable = true
        isFocusable = false

        contentView = LayoutInflater.from(context).inflate(R.layout.popup_address_tooltip, null)

        contentView.findViewById<TextView>(R.id.address).text = address.formatEthAddressBold()
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        width = ViewGroup.LayoutParams.WRAP_CONTENT

        contentView.setOnClickListener {
            dismiss()
        }
    }
}
