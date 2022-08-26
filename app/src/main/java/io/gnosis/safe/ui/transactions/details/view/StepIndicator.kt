package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewStepIndicatorRejectionStepBinding
import io.gnosis.safe.utils.dpToPx
import pm.gnosis.svalinn.common.utils.getColorCompat
import timber.log.Timber

class StepIndicator @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val edgesPaint: Paint

    init {
        orientation = HORIZONTAL
        edgesPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        edgesPaint.style = Paint.Style.FILL_AND_STROKE
        edgesPaint.strokeWidth = dpToPx(2).toFloat()
        edgesPaint.color = context.getColorCompat(R.color.label_tertiary)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        for (i in 0 until childCount - 1) {

            val child1 = getChildAt(i) as Step
            val child2 = getChildAt(i + 1) as Step

            canvas.drawLine(
                child1.connectorPointRight.x,
                child1.connectorPointRight.y,
                child2.connectorPointLeft.x,
                child2.connectorPointLeft.y,
                edgesPaint
            )
        }
    }
}

interface Step {
    val connectorPointRight: PointF
    val connectorPointLeft: PointF
}

class RejectionStep @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), Step {

    private val binding: ViewStepIndicatorRejectionStepBinding

    init {
        binding = ViewStepIndicatorRejectionStepBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL
        gravity = Gravity.CENTER
        readAttributesAndSetupFields(context, attrs)
    }

    private var connectorMargin: Float = 0f

    override val connectorPointRight: PointF
        get() = PointF(left + binding.stepIcon.right.toFloat() + connectorMargin, top + binding.stepIcon.height.toFloat() / 2)
    override val connectorPointLeft: PointF
        get() = PointF(left + binding.stepIcon.left.toFloat() - connectorMargin, top + binding.stepIcon.height.toFloat() / 2)

    private fun readAttributesAndSetupFields(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.StepIndicator,
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
            connectorMargin = a.getDimension(R.styleable.StepIndicator_step_connector_margin, 0f)
            stepIcon.setImageResource(a.getResourceId(R.styleable.StepIndicator_step_icon, -1))
            stepLabel.text = a.getString(R.styleable.StepIndicator_step_text)
            val isActiveStep = a.getBoolean(R.styleable.StepIndicator_step_active, true)
            stepLabel.setTextColor(context.getColorCompat(if (isActiveStep) R.color.label_primary else R.color.label_tertiary))
        }
    }
}
