package io.gnosis.safe.ui.safe.settings.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewEndpointItemBinding
import timber.log.Timber

class EndpointItem @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewEndpointItemBinding.inflate(LayoutInflater.from(context), this)

    init {
        readAttributesAndSetupFields(context, attrs)
    }

    var name: String? = null
        set(value) {
            binding.name.text = value
            field = value
        }

    var value: String? = null
        set(value) {
            binding.value.text = value
            field = value
        }

    private fun readAttributesAndSetupFields(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.EndpointItem,
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
        name = a.getString(R.styleable.EndpointItem_endpoint_name)
        value = a.getString(R.styleable.EndpointItem_endpoint_value)
    }
}

