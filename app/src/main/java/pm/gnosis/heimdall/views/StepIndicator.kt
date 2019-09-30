package pm.gnosis.heimdall.views

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import pm.gnosis.heimdall.R
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.android.synthetic.main.view_step_indicator_step.view.*
import android.graphics.Shader.TileMode
import android.util.SparseArray
import kotlin.math.min


class StepIndicator @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val backgroundPaint: Paint

    private val inactiveEdgeColor = ContextCompat.getColor(context, R.color.disabled_button)
    private val activeEdgeColor = ContextCompat.getColor(context, R.color.safe_green)

    private val edgeShaders: SparseArray<LinearGradient> = SparseArray()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        backgroundPaint.style = Paint.Style.FILL_AND_STROKE
        backgroundPaint.strokeWidth = resources.getDimension(R.dimen.step_indicator_edge_width)
    }

    fun updateStep(index: Int, stepState: StepState) {
        val step = getChildAt(index) as Step
        step.state = stepState
        edgeShaders.clear()
        invalidate()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (changed) {
            var childsMargin = if (childCount == 0) 0 else (width - paddingLeft * 2 - getChildAt(0).measuredWidth * childCount) / childCount
            var left = l + paddingLeft + childsMargin / 2
            for (i in 0 until childCount) {
                val child = getChildAt(i)

                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight

                child.layout(left + childsMargin / 2, paddingTop, left + childWidth + childsMargin / 2, childHeight + paddingTop)
                left += childWidth + childsMargin / 2
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        edgeShaders.clear()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        for (i in 0 until childCount - 1) {

            val child1 = getChildAt(i) as Step
            val child2 = getChildAt(i + 1) as Step

            if (edgeShaders.size() <= i) {
                edgeShaders.put(
                    i, LinearGradient(
                        child1.getCircleCenter().x + child1.radius(),
                        child1.getCircleCenter().y,
                        child2.getCircleCenter().x - child2.radius(),
                        child2.getCircleCenter().y,
                        if (child1.isActive) activeEdgeColor else inactiveEdgeColor,
                        if (child2.isActive) activeEdgeColor else inactiveEdgeColor,
                        TileMode.CLAMP
                    )
                )
            }

            backgroundPaint.shader = edgeShaders[i]

            canvas.drawLine(
                child1.getCircleCenter().x + child1.radius(),
                child1.getCircleCenter().y,
                child2.getCircleCenter().x - child2.radius(),
                child2.getCircleCenter().y,
                backgroundPaint
            )
        }
    }
}

enum class StepState {
    UNCOMPLETED_INACTIVE,
    UNCOMPLETED_ACTIVE,
    SKIPPED,
    COMPLETED;

    companion object {

        fun from(value: Int): StepState = when (value) {
            1 -> UNCOMPLETED_ACTIVE
            2 -> SKIPPED
            3 -> COMPLETED
            else -> UNCOMPLETED_INACTIVE
        }
    }
}

class Step @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var state: StepState = StepState.UNCOMPLETED_INACTIVE
        set(value) {
            circle.state = value
            when (value) {
                StepState.UNCOMPLETED_INACTIVE, StepState.SKIPPED -> {
                    label.isEnabled = false
                }
                else -> {
                    label.isEnabled = true
                }
            }
            field = value
        }

    val isActive: Boolean
        get() = state == StepState.UNCOMPLETED_ACTIVE || state == StepState.COMPLETED

    var number: Int = 1
        set(value) {
            circle.number = value
            field = value
            invalidate()
        }

    var text: String = ""
        set(value) {
            label.text = value
            field = value
        }

    private val circle: StepCircle
    private val label: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.view_step_indicator_step, this, true)

        circle = step_circle
        label = step_label

        orientation = VERTICAL
        gravity = Gravity.CENTER

        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.StepIndicator,
            0, 0
        )
        try {
            state = StepState.from(a.getInt(R.styleable.StepIndicator_stepState, 0))
            number = a.getInt(R.styleable.StepIndicator_number, 1)
            text = a.getString(R.styleable.StepIndicator_label)

        } finally {
            a.recycle()
        }

        val colorStateList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_enabled), intArrayOf(-android.R.attr.state_enabled)),
            intArrayOf(ContextCompat.getColor(context, R.color.safe_green), ContextCompat.getColor(context, R.color.disabled_button))
        )
        label.setTextColor(colorStateList)
    }

    fun getCircleCenter(): PointF {
        return PointF(left + circle.left + radius(), top + circle.top + radius())
    }

    fun radius(): Float = circle.width.toFloat() / 2
}

class StepCircle @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint: Paint
    private val outlinePaint: Paint
    private val symbolPaint: Paint
    private val textPaint: Paint

    private val numberRect = Rect()

    var state: StepState = StepState.UNCOMPLETED_INACTIVE
        set(value) {
            when (value) {
                StepState.UNCOMPLETED_INACTIVE -> {
                    backgroundPaint.color = ContextCompat.getColor(context, R.color.white)
                    outlinePaint.color = ContextCompat.getColor(context, R.color.disabled_button)
                    textPaint.color = ContextCompat.getColor(context, R.color.disabled_button)
                }
                StepState.UNCOMPLETED_ACTIVE -> {
                    backgroundPaint.color = ContextCompat.getColor(context, R.color.white)
                    outlinePaint.color = ContextCompat.getColor(context, R.color.safe_green)
                    textPaint.color = ContextCompat.getColor(context, R.color.safe_green)
                }
                StepState.SKIPPED -> {
                    backgroundPaint.color = ContextCompat.getColor(context, R.color.disabled_button)
                    outlinePaint.color = ContextCompat.getColor(context, R.color.disabled_button)
                    textPaint.color = ContextCompat.getColor(context, R.color.disabled_button)
                }
                StepState.COMPLETED -> {
                    backgroundPaint.color = ContextCompat.getColor(context, R.color.safe_green)
                    outlinePaint.color = ContextCompat.getColor(context, R.color.safe_green)
                    textPaint.color = ContextCompat.getColor(context, R.color.safe_green)
                }
            }
            field = value
        }

    var number: Int = 1

    init {

        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        backgroundPaint.style = Paint.Style.FILL
        backgroundPaint.color = ContextCompat.getColor(context, R.color.white)

        outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.color = ContextCompat.getColor(context, R.color.disabled_button)
        outlinePaint.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics)

        symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        symbolPaint.style = Paint.Style.STROKE
        symbolPaint.color = ContextCompat.getColor(context, R.color.white)
        symbolPaint.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics)

        textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint.style = Paint.Style.FILL_AND_STROKE
        textPaint.color = ContextCompat.getColor(context, R.color.disabled_button)
        textPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10f, resources.displayMetrics)

        readAttributesAndSetupFields(context, attrs)
    }

    private fun readAttributesAndSetupFields(context: Context, attrs: AttributeSet?) {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.StepIndicator,
            0, 0
        )
        try {
            applyAttributes(context, a)
        } finally {
            a.recycle()
        }
    }

    private fun applyAttributes(context: Context, a: TypedArray) {
        number = a.getInt(R.styleable.StepIndicator_number, 1)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        textPaint.getTextBounds(number.toString(), 0, number.toString().length, numberRect)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width.toFloat() / 2
        val cy = height.toFloat() / 2
        val radius = min(width.toFloat() / 2, height.toFloat() / 2)

        // draw step circle shape
        canvas.drawCircle(cx, cy, radius, backgroundPaint)
        canvas.drawCircle(cx, cy, radius - outlinePaint.strokeWidth, outlinePaint)

        when (state) {

            StepState.UNCOMPLETED_ACTIVE, StepState.UNCOMPLETED_INACTIVE -> {
                //FIXME: preivew in android studio shows incorrect vertical alignment of this number label; on phone it works though
                canvas.drawText(number.toString(), cx - numberRect.width().toFloat() / 2, cy + numberRect.height().toFloat() / 2, textPaint)
            }
            StepState.SKIPPED -> {
                // draw x
                val dx = 0.7f * radius * cos(Math.toRadians(45.toDouble())).toFloat()
                val dy = 0.7f * radius * sin(Math.toRadians(45.toDouble())).toFloat()
                canvas.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, symbolPaint)
                canvas.drawLine(cx + dx, cy - dy, cx - dx, cy + dy, symbolPaint)
            }
            StepState.COMPLETED -> {
                // draw checkmark
                val leftDelta = radius * 0.4f * cos(Math.toRadians(45.toDouble())).toFloat()
                val rightDelta = radius * 0.6f * sin(Math.toRadians(45.toDouble())).toFloat()
                canvas.drawLine(cx - leftDelta, cy - leftDelta + leftDelta * 0.5f, cx, cy + leftDelta * 0.5f, symbolPaint)
                canvas.drawLine(cx, cy + leftDelta * 0.5f, cx + rightDelta, cy - rightDelta + leftDelta * 0.5f, symbolPaint)
            }
        }
    }
}




