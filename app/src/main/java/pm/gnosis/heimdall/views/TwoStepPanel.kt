package pm.gnosis.heimdall.views

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.ImageViewCompat
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import kotlinx.android.synthetic.main.layout_two_step_panel.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.utils.setColorFilterCompat
import pm.gnosis.svalinn.common.utils.getColorCompat


class TwoStepPanel @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var disabled: Boolean = false
        set(value) {
            field = value
            isEnabled = !disabled
            setForwardEnabled(!disabled)
            setBackEnabled(!disabled)
            refreshDrawableState()
        }

    var step: Step = Step.ONE
        set(value) {
            field = value
            when (value) {

                Step.NONE -> {
                    indicator_step_1.visibility = View.GONE
                    indicator_step_2.visibility = View.GONE
                    back_icon.visibility = View.GONE
                }

                Step.ONE, Step.TWO -> {

                }

            }
            refreshDrawableState()
        }

    var forwardLabel: String = ""
        set(value) {
            field = value
            forward_text.text = value
        }

    private var forwardEnabled = true
    private var backEnabled = true

    val forwardClicks: Observable<Unit>
    val backClicks: Observable<Unit>

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_two_step_panel, this, true)
        readAttributesAndSetupFields(context, attrs)

        forwardClicks = forward.clicks().filter { forwardEnabled }
        backClicks = back_icon.clicks().filter { backEnabled }
    }

    private fun readAttributesAndSetupFields(context: Context, attrs: AttributeSet?) {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.TwoStepPanel,
            0, 0
        )
        try {
            applyAttributes(context, a)
        } finally {
            a.recycle()
        }
    }

    private fun applyAttributes(context: Context, a: TypedArray) {
        step = Step.from(a.getInt(R.styleable.TwoStepPanel_step, 0))
        forwardLabel = a.getString(R.styleable.TwoStepPanel_forwardLabel) ?: ""
        disabled = a.getBoolean(R.styleable.TwoStepPanel_disabled, false)

        setForwardEnabled(a.getBoolean(R.styleable.TwoStepPanel_forwardEnabled, true))
        setBackEnabled(a.getBoolean(R.styleable.TwoStepPanel_backEnabled, true))

        val textColorEnabled = a.getColor(R.styleable.TwoStepPanel_colorTextEnabled, context.getColorCompat(R.color.white))
        val textColorDisabled = a.getColor(R.styleable.TwoStepPanel_colorTextDisabled, context.getColorCompat(R.color.dark_grey))

        val colorStateList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_enabled), intArrayOf(-android.R.attr.state_enabled)),
            intArrayOf(textColorEnabled, textColorDisabled)
        )

        forward_text.setTextColor(colorStateList)
        ImageViewCompat.setImageTintList(back_icon, colorStateList)
        ImageViewCompat.setImageTintList(forward_icon, colorStateList)

        val backVisible = a.getBoolean(R.styleable.TwoStepPanel_backVisible, true)
        back_icon.visibility = if (backVisible) View.VISIBLE else View.GONE
    }

    fun setForwardEnabled(enabled: Boolean) {
        forwardEnabled = enabled
        forward_text.isEnabled = enabled
        forward_icon.setColorFilterCompat(if (enabled) R.color.white else R.color.medium_grey)
    }

    fun setBackEnabled(enabled: Boolean) {
        backEnabled = enabled
        back_icon.setColorFilterCompat(if (enabled) R.color.white else R.color.medium_grey)
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val state = super.onCreateDrawableState(extraSpace + 1)
        if (disabled) {
            mergeDrawableStates(state, DISABLED_STATE)
        }
        return state
    }

    override fun refreshDrawableState() {
        super.refreshDrawableState()

        when (step) {
            Step.ONE -> {
                changeShapeColor(
                    indicator_step_1,
                    context.getColorCompat(
                        if (!disabled) {
                            R.color.white
                        } else {
                            R.color.dark_grey
                        }
                    )
                )
                changeShapeColor(
                    indicator_step_2,
                    context.getColorCompat(
                        if (!disabled) {
                            R.color.medium_grey
                        } else {
                            R.color.medium_grey
                        }
                    )
                )
            }
            Step.TWO -> {
                changeShapeColor(
                    indicator_step_1,
                    context.getColorCompat(
                        if (!disabled) {
                            R.color.medium_grey
                        } else {
                            R.color.medium_grey
                        }
                    )
                )
                changeShapeColor(
                    indicator_step_2,
                    context.getColorCompat(
                        if (!disabled) {
                            R.color.white
                        } else {
                            R.color.dark_grey
                        }
                    )
                )
            }
            Step.NONE -> { /*NOOP*/ }
        }
    }

    private fun changeShapeColor(view: View, color: Int) {
        when (val background = view.background) {
            is ShapeDrawable -> background.paint.color = color
            is GradientDrawable -> background.setColor(color)
            is ColorDrawable -> background.color = color
        }
    }

    enum class Step {
        NONE,
        ONE,
        TWO;

        companion object {

            fun from(value: Int): Step = when (value) {
                1 -> ONE
                2 -> TWO
                else -> NONE
            }
        }
    }

    companion object {
        val DISABLED_STATE = intArrayOf(R.attr.state_disabled)
    }

}