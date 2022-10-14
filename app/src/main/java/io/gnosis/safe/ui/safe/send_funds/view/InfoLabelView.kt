package io.gnosis.safe.ui.safe.send_funds.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import io.gnosis.safe.R
import io.gnosis.safe.utils.dpToPx
import timber.log.Timber

class InfoLabelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        readAttributesAndSetupFields(context, attrs)
    }

    private fun readAttributesAndSetupFields(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.InfoLabelView,
            0, 0
        ).also {
            runCatching {
                applyAttributes(context, it)
            }
                .onFailure { Timber.e(it) }
            it.recycle()
        }
    }

    private fun applyAttributes(context: Context, a: TypedArray) {
        val infoIconResId = a.getResourceId(R.styleable.InfoLabelView_info_label_icon, R.drawable.ic_info_16dp)
        setCompoundDrawablesWithIntrinsicBounds(0, 0, infoIconResId, 0)
        compoundDrawablePadding = dpToPx(6)
        val infoText = a.getString(R.styleable.InfoLabelView_info_label_text)
        infoText?.let {
            setOnClickListener {
                Tooltip(context, infoText).showAsDropDown(it)
            }
        }
    }
}
