package io.gnosis.safe.ui.settings.view

import android.content.Context
import android.content.res.TypedArray
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewSettingsItemBinding
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber

class SettingItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy {
        ViewSettingsItemBinding.inflate(
            LayoutInflater.from(context),
            this
        )
    }

    init {
        readAttributesAndSetupFields(context, attrs)
    }

    var openable: Boolean = true
        set(value) {
            binding.arrow.visible(value)
            field = value
        }

    var hasSwitch: Boolean = false
        set(value) {
            binding.settingSwitch.visible(value)
            field = value
        }

    var checked: Boolean = false
        set(value) {
            binding.checkMark.visible(value)
            field = value
        }

    var name: CharSequence? = null
        set(value) {
            binding.name.text = value
            field = value
        }

    var description: CharSequence? = null
        set(value) {
            binding.description.visible(!value.isNullOrBlank())
            binding.description.text = value
            field = value
        }

    var value: CharSequence? = null
        set(value) {
            binding.value.text = value
            field = value
        }

    var subValue: CharSequence? = null
        set(value) {
            binding.value.setTextColor(ContextCompat.getColor(context, R.color.label_primary))
            binding.subValue.visible(!value.isNullOrBlank())
            binding.subValue.text = value
            field = value
        }

    var settingImage: Int? = null
        set(value) {
            value?.let {
                binding.image.setImageResource(value)
            }
            field = value
        }

    private fun readAttributesAndSetupFields(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SettingItem,
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
        checked = a.getBoolean(R.styleable.SettingItem_setting_checked, false)
        hasSwitch = a.getBoolean(R.styleable.SettingItem_setting_has_switch, false)
        openable = a.getBoolean(R.styleable.SettingItem_setting_openable, true)
        name = a.getString(R.styleable.SettingItem_setting_name)
        description = a.getString(R.styleable.SettingItem_setting_description)
        value = a.getString(R.styleable.SettingItem_setting_value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.value.setTextAppearance(a.getResourceId(R.styleable.SettingItem_setting_value_style, R.style.TextMedium))
        } else {
            binding.value.setTextAppearance(context, a.getResourceId(R.styleable.SettingItem_setting_value_style, R.style.TextMedium))
        }
        val imageResId = a.getResourceId(R.styleable.SettingItem_setting_image, -1)
        if (imageResId > 0) {
            binding.image.setImageResource(imageResId)
        } else {
            binding.image.visible(false)
        }
    }

    val settingSwitch = binding.settingSwitch
}
