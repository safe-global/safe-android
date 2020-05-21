package io.gnosis.safe.ui.safe.settings.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewEndpointItemBinding

class EndpointItem @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewEndpointItemBinding.inflate(LayoutInflater.from(context), this)

    var name: String? = null
        set(value) {
            binding.name.text = name
            field = value
        }

    var value: String? = null
        set(value) {
            binding.value.text = value
            field = value
        }

    init {
        readAttributesAndSetupFields(context, attrs)
    }

    private fun readAttributesAndSetupFields(context: Context, attrs: AttributeSet?) {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.EndpointItem,
            0, 0
        )
        try {
            applyAttributes(context, a)

        } finally {
            a.recycle()
        }
    }

    private fun applyAttributes(context: Context, a: TypedArray) {
        name = a.getString(R.styleable.SettingItem_setting_name)
        value = a.getString(R.styleable.SettingItem_setting_value)
    }
}

