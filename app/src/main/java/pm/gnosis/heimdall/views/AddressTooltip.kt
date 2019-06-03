package pm.gnosis.heimdall.views

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
import pm.gnosis.heimdall.R

class AddressTooltip(
    val context: Context,
    address: String
) : PopupWindow(context) {

    init {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        isOutsideTouchable = true
        isFocusable = false

        contentView = LayoutInflater.from(context).inflate(R.layout.popup_address_tooltip, null)

        //make first & last 4 characters bold
        val addressString=  SpannableStringBuilder(address.substring(0, address.length / 2) + "\n" + address.substring(address.length / 2, address.length))
        addressString.setSpan(StyleSpan(Typeface.BOLD), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        addressString.setSpan(StyleSpan(Typeface.BOLD), addressString.length - 4, addressString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        contentView.findViewById<TextView>(R.id.address).text = addressString
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        width = ViewGroup.LayoutParams.WRAP_CONTENT

        contentView.setOnClickListener {
            dismiss()
        }
    }
}


