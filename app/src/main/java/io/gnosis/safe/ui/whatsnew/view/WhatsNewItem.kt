package io.gnosis.safe.ui.whatsnew.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewWhatsNewItemBinding
import timber.log.Timber

class WhatsNewItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewWhatsNewItemBinding

    init {
        binding = ViewWhatsNewItemBinding.inflate(LayoutInflater.from(context), this, true)
        readAttributesAndSetupFields(context, attrs)
    }

    private fun readAttributesAndSetupFields(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.WhatsNewItem,
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
            itemImage.setImageResource(a.getResourceId(R.styleable.WhatsNewItem_whatsnew_icon, -1))
            itemTitle.text = a.getString(R.styleable.WhatsNewItem_whatsnew_title)
            itemDescription.text = a.getString(R.styleable.WhatsNewItem_whatsnew_description)
        }
    }
}
