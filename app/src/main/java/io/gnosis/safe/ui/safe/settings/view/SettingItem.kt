package io.gnosis.safe.ui.safe.settings.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewSettingsItemBinding
import pm.gnosis.svalinn.common.utils.visible

class SettingItem @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewSettingsItemBinding.inflate(LayoutInflater.from(context), this) }

    var openable: Boolean = true
        set(value) {
            binding.arrow.visible(value)
            field = value
        }

    var name: CharSequence? = null
        set(value) {
            binding.name.text = value
            field = value
        }

    var value: CharSequence? = null
        set(value) {
            binding.value.text = value
            field = value
        }

    init {
        readAttributesAndSetupFields(context, attrs)
    }

    private fun readAttributesAndSetupFields(context: Context, attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.SettingItem,
            0, 0
        )
        try {
            applyAttributes(context, a)

        } finally {
            a.recycle()
        }
    }

    private fun applyAttributes(context: Context, a: TypedArray) {
        openable = a.getBoolean(R.styleable.SettingItem_setting_openable, true)
        name = a.getString(R.styleable.SettingItem_setting_name)
        value = a.getString(R.styleable.SettingItem_setting_value)
        val imageResId = a.getResourceId(R.styleable.SettingItem_setting_image, -1)
        if (imageResId > 0) {
            binding.image.setImageResource(imageResId)
        } else {
            binding.image.visible(false)
        }
    }
}
