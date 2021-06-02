package io.gnosis.safe.ui.settings.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewSeedItemBinding
import timber.log.Timber

class SeedItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewSeedItemBinding.inflate(LayoutInflater.from(context), this) }

    init {
        readAttributesAndSetupFields(context, attrs)
    }

    var number: Int = 0
        set(value) {
            binding.number.text = value.toString()
            field = value
        }

    var word: String? = null
        set(value) {
            binding.seedWord.text = value
            field = value
        }

    private fun readAttributesAndSetupFields(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SeedItem,
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
            number.text = a.getString(R.styleable.SeedItem_seed_number)
            seedWord.text = a.getInteger(R.styleable.SeedItem_seed_number, 0).toString()
        }
    }
}
