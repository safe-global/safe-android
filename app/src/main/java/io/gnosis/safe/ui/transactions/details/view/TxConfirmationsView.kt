package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import io.gnosis.data.models.TransactionStatus
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewTxConfirmationsSectionHeaderBinding
import io.gnosis.safe.ui.settings.view.AddressItem
import io.gnosis.safe.utils.dpToPx
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.getColorCompat

class TxConfirmationsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var status: TransactionStatus = TransactionStatus.AWAITING_CONFIRMATIONS
    var threshold: Int = 0
    var executor: Solidity.Address? = null
    private val confirmations = mutableListOf<Solidity.Address>()

    private val linePaint: Paint

    init {
        orientation = VERTICAL
        gravity = Gravity.LEFT
        linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        linePaint.style = Paint.Style.FILL_AND_STROKE
        linePaint.strokeWidth = dpToPx(LINE_WIDTH).toFloat()
        linePaint.color = context.getColorCompat(LINE_COLOR)
    }

    fun setConfirmations(addresses: List<Solidity.Address>) {

        clear()

        addView(SectionHeader(context).apply {
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, dpToPx(MARGIN_VERTICAL))
            layoutParams = lp
            update(SectionHeader.Type.CREATED)
        })

        addresses.forEach {
            addView(SectionHeader(context).apply {
                val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 0, 0, dpToPx(MARGIN_VERTICAL))
                layoutParams = lp
                update(SectionHeader.Type.CONFIRMED)
            })
            val confirmationItem = AddressItem(context)
            val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(ADDRESS_ITEM_HEIGHT))
            layoutParams.setMargins(dpToPx(ADDRESS_ITEM_MARGIN_LEFT), 0, 0, dpToPx(MARGIN_VERTICAL))
            confirmationItem.layoutParams = layoutParams
            confirmationItem.address = it
            addView(confirmationItem)
        }

        if (confirmations.size < threshold) {
            addView(SectionHeader(context).apply {
                update(SectionHeader.Type.EXECUTE_WAITING, threshold - confirmations.size)
            })
        } else {

            when (status) {
                TransactionStatus.AWAITING_EXECUTION -> {
                    addView(SectionHeader(context).apply {
                        update(SectionHeader.Type.EXECUTE_READY)
                    })
                }
                TransactionStatus.SUCCESS -> {
                    addView(SectionHeader(context).apply {
                        update(SectionHeader.Type.EXECUTE_DONE)
                    })
                    val executorItem = AddressItem(context)
                    val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(ADDRESS_ITEM_HEIGHT))
                    layoutParams.setMargins(dpToPx(ADDRESS_ITEM_MARGIN_LEFT), dpToPx(MARGIN_VERTICAL), 0, 0)
                    executorItem.layoutParams = layoutParams
                    executorItem.address = executor
                    addView(executorItem)
                }
                TransactionStatus.CANCELLED -> {
                    addView(SectionHeader(context).apply {
                        update(SectionHeader.Type.CANCELED)
                    })
                }
                TransactionStatus.FAILED -> {
                    addView(SectionHeader(context).apply {
                        update(SectionHeader.Type.FAILED)
                    })
                }
            }
        }
    }

    fun clear() {
        removeAllViews()
        confirmations.clear()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        var x1 = 0f
        var y1 = 0f
        for (i in 0 until childCount - 1) {

            val child1 = getChildAt(i)
            if (child1 is SectionHeader) {
                x1 = child1.getCircleBottom().x
                y1 = child1.getCircleBottom().y
            }

            val child2 = getChildAt(i + 1)
            if (child2 is SectionHeader) {
                canvas.drawLine(
                    x1,
                    y1,
                    child2.getCircleTop().x,
                    child2.getCircleTop().y,
                    linePaint
                )
            }
        }
    }

    class SectionHeader @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : ConstraintLayout(context, attrs, defStyleAttr) {

        var type: Type = Type.CREATED
            private set

        enum class Type {
            CREATED,
            CONFIRMED,
            CANCELED,
            FAILED,
            EXECUTE_WAITING,
            EXECUTE_READY,
            EXECUTE_DONE
        }

        private val binding by lazy { ViewTxConfirmationsSectionHeaderBinding.inflate(LayoutInflater.from(context), this) }

        fun update(type: Type, missingConfirmations: Int? = null) {
            this.type = type
            with(binding) {
                when (type) {
                    Type.CREATED -> {
                        sectionIcon.setImageResource(R.drawable.ic_tx_confirmations_start_16dp)
                        sectionTitle.text = resources.getString(R.string.tx_confirmations_created)
                        sectionTitle.setTextColor(ContextCompat.getColor(context, R.color.safe_green))
                    }
                    Type.CONFIRMED -> {
                        sectionIcon.setImageResource(R.drawable.ic_tx_confirmations_done_16dp)
                        sectionTitle.text = resources.getString(R.string.tx_confirmations_confirmed)
                        sectionTitle.setTextColor(ContextCompat.getColor(context, R.color.safe_green))
                    }
                    Type.CANCELED -> {
                        sectionIcon.setImageResource(R.drawable.ic_tx_confirmations_canceled_16dp)
                        sectionTitle.text = resources.getString(R.string.tx_confirmations_canceled)
                        sectionTitle.setTextColor(ContextCompat.getColor(context, R.color.gnosis_dark_blue))
                    }
                    Type.FAILED -> {
                        sectionIcon.setImageResource(R.drawable.ic_tx_confirmations_failed_16dp)
                        sectionTitle.text = resources.getString(R.string.tx_confirmations_failed)
                        sectionTitle.setTextColor(ContextCompat.getColor(context, R.color.tomato))
                    }
                    Type.EXECUTE_WAITING -> {
                        sectionIcon.setImageResource(R.drawable.ic_tx_confirmations_waiting_16dp)
                        sectionTitle.text = resources.getString(R.string.tx_confirmations_execute_waiting, missingConfirmations)
                        sectionTitle.setTextColor(ContextCompat.getColor(context, R.color.medium_grey))
                    }
                    Type.EXECUTE_READY -> {
                        sectionIcon.setImageResource(R.drawable.ic_tx_confirmations_ready_16dp)
                        sectionTitle.text = resources.getString(R.string.tx_confirmations_execute_ready)
                        sectionTitle.setTextColor(ContextCompat.getColor(context, R.color.safe_green))
                    }
                    Type.EXECUTE_DONE -> {
                        sectionIcon.setImageResource(R.drawable.ic_tx_confirmations_done_16dp)
                        sectionTitle.text = resources.getString(R.string.tx_confirmations_executed)
                        sectionTitle.setTextColor(ContextCompat.getColor(context, R.color.safe_green))
                    }
                }
            }
        }

        fun getCircleBottom(): PointF {
            return PointF(left + binding.sectionIcon.left + binding.sectionIcon.width.toFloat() / 2, top + binding.sectionIcon.bottom.toFloat())
        }

        fun getCircleTop(): PointF {
            return PointF(left + binding.sectionIcon.left + binding.sectionIcon.width.toFloat() / 2, top + binding.sectionIcon.top.toFloat())
        }
    }

    companion object {
        private const val ADDRESS_ITEM_HEIGHT = 44
        private const val ADDRESS_ITEM_MARGIN_LEFT = 24
        private const val MARGIN_VERTICAL = 16
        private const val LINE_WIDTH = 2
        private const val LINE_COLOR = R.color.medium_grey
    }
}

