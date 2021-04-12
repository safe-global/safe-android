package io.gnosis.safe.ui.settings.owner.intro

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.cardview.widget.CardView
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewInfoItemBinding
import io.gnosis.safe.utils.appendLink
import timber.log.Timber

class InfoItemView  @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private val binding = ViewInfoItemBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        readAttributesAndSetupFields(context, attrs)
    }

    private fun readAttributesAndSetupFields(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.InfoItem,
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
        with(binding) {
            introItemIcon.setImageResource(a.getResourceId(R.styleable.InfoItem_info_icon, -1))
            introItemTitle.text = a.getString(R.styleable.InfoItem_info_title)
            introItemText.text = a.getString(R.styleable.InfoItem_info_text)
        }
    }

    fun addInfoLink(urlText: String, url: String) {
        with(binding) {
            introItemText.appendLink(
                urlText = urlText,
                url = url,
                linkIcon = R.drawable.ic_external_link_green_16dp,
                prefix = "\n"
            )
        }
    }
}
