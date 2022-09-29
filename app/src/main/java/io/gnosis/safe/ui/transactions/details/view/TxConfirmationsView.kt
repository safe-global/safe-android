package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import io.gnosis.data.models.Chain
import io.gnosis.data.models.Owner
import io.gnosis.data.models.transaction.TransactionStatus
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewTxConfirmationsExecutionStepBinding
import io.gnosis.safe.ui.settings.view.AddressItem
import io.gnosis.safe.ui.settings.view.NamedAddressItem
import io.gnosis.safe.utils.dpToPx
import io.gnosis.safe.utils.shortChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.getColorCompat
import timber.log.Timber


class TxConfirmationsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val linePaint: Paint

    private val dp1 = dpToPx(1)

    init {
        orientation = VERTICAL
        gravity = Gravity.LEFT
        linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        linePaint.style = Paint.Style.FILL_AND_STROKE
        linePaint.strokeWidth = dpToPx(LINE_WIDTH).toFloat()
        linePaint.color = context.getColorCompat(LINE_COLOR)
    }

    fun setExecutionData(chain: Chain, rejection: Boolean, status: TransactionStatus, confirmations: List<Solidity.Address>, threshold: Int, executor: Solidity.Address? = null, localOwners: List<Owner> = listOf()) {

        clear()

        if (rejection) {
            addExecutionStep(TxExecutionStep.Type.REJECTION_CREATED)
        } else {
            addExecutionStep(TxExecutionStep.Type.CREATED)
        }

        confirmations.forEach { confirmationAddress ->
            addExecutionStep(TxExecutionStep.Type.CONFIRMED)

            val localOwner = localOwners.find { it.address == confirmationAddress }
            if (localOwner != null) {
                addNamedAddressItem(chain, localOwner.address, localOwner.name, localOwner.type)
            } else {
                addAddressItem(chain, confirmationAddress)
            }
        }

        when (status) {
            TransactionStatus.AWAITING_CONFIRMATIONS -> {
                addExecutionStep(TxExecutionStep.Type.EXECUTE_WAITING, threshold - confirmations.size)
            }
            TransactionStatus.AWAITING_EXECUTION -> {
                addExecutionStep(TxExecutionStep.Type.EXECUTE_READY)
                addExecutionDescriptionItem()
            }
            TransactionStatus.SUCCESS -> {
                if (executor != null) {

                    addExecutionStep(TxExecutionStep.Type.EXECUTE_DONE)

                    val localOwner = localOwners.find { it.address == executor }
                    if (localOwner != null) {
                        addNamedAddressItem(chain, localOwner.address, localOwner.name, localOwner.type)
                    } else {
                        addAddressItem(chain, executor)
                    }

                } else {
                    // fail silently - this should never happen
                    Timber.e("${TxConfirmationsView::class.java.simpleName}: missing executor for successful transaction")
                    addExecutionStep(TxExecutionStep.Type.EXECUTE_READY)
                }
            }
            TransactionStatus.CANCELLED -> {
                addExecutionStep(TxExecutionStep.Type.CANCELLED)
            }
            TransactionStatus.FAILED -> {
                addExecutionStep(TxExecutionStep.Type.FAILED)
            }
        }
    }

    private fun addExecutionStep(type: TxExecutionStep.Type, missingConfirmations: Int? = null) {
        addView(TxExecutionStep(context).apply {
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, dpToPx(MARGIN_VERTICAL))
            layoutParams = lp
            setStep(type, missingConfirmations)
        })
    }

    private fun addAddressItem(chain: Chain, address: Solidity.Address) {
        val addressItem = AddressItem(context)
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(ADDRESS_ITEM_HEIGHT))
        layoutParams.setMargins(dpToPx(ADDRESS_ITEM_MARGIN_LEFT), 0, 0, dpToPx(MARGIN_VERTICAL))
        addressItem.layoutParams = layoutParams
        addressItem.setAddress(chain, address)
        addView(addressItem)
    }

    private fun addNamedAddressItem(chain: Chain, address: Solidity.Address, name: String?, ownerType : Owner.Type? = null) {
        val namedAddressItem = NamedAddressItem(context)
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(ADDRESS_ITEM_HEIGHT))
        layoutParams.setMargins(dpToPx(ADDRESS_ITEM_MARGIN_LEFT), 0, 0, dpToPx(MARGIN_VERTICAL))
        namedAddressItem.layoutParams = layoutParams
        namedAddressItem.background = ContextCompat.getDrawable(context, R.drawable.background_secondary_selectable)
        namedAddressItem.setAddress(chain, address, ownerType)
        namedAddressItem.name = if (name.isNullOrBlank())
            resources.getString(
                R.string.settings_app_imported_owner_key_default_name,
                address.shortChecksumString()
            ) else name
        addView(namedAddressItem)
    }

    private fun addExecutionDescriptionItem() {
        val item = TextView(context)
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(
            dpToPx(EXECUTION_DESCRIPTION_ITEM_MARGIN_LEFT),
            dpToPx(EXECUTION_DESCRIPTION_ITEM_MARGIN_VERTICAL),
            dpToPx(EXECUTION_DESCRIPTION_ITEM_MARGIN_RIGHT),
            dpToPx(MARGIN_VERTICAL)
        )
        item.layoutParams = layoutParams
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            item.setTextAppearance(R.style.TextDark_Bold)
        } else {
            item.setTextAppearance(context, R.style.TextDark_Bold)
        }
        item.text = resources.getString(R.string.tx_confirmations_execute_ready_description)
        addView(item)
    }

    fun clear() {
        removeAllViews()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        var x1 = 0f
        var y1 = 0f
        for (i in 0 until childCount - 1) {

            val child1 = getChildAt(i)
            if (child1 is TxExecutionStep) {
                x1 = child1.stepIconBottom.x
                y1 = child1.stepIconBottom.y + dp1
            }

            val child2 = getChildAt(i + 1)
            if (child2 is TxExecutionStep) {
                canvas.drawLine(
                    x1,
                    y1,
                    child2.stepIconTop.x,
                    child2.stepIconTop.y - dp1,
                    linePaint
                )
            }
        }
    }

    private class TxExecutionStep @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : ConstraintLayout(context, attrs, defStyleAttr) {

        var type: Type = Type.CREATED
            private set

        enum class Type {
            CREATED,
            CONFIRMED,
            CANCELLED,
            FAILED,
            EXECUTE_WAITING,
            EXECUTE_READY,
            EXECUTE_DONE,
            REJECTION_CREATED
        }

        private val binding by lazy { ViewTxConfirmationsExecutionStepBinding.inflate(LayoutInflater.from(context), this) }

        val stepIconBottom: PointF
            get() = PointF(left + binding.stepIcon.left + binding.stepIcon.width.toFloat() / 2, top + binding.stepIcon.bottom.toFloat())

        val stepIconTop: PointF
            get() = PointF(left + binding.stepIcon.left + binding.stepIcon.width.toFloat() / 2, top + binding.stepIcon.top.toFloat())

        fun setStep(type: Type, missingConfirmations: Int? = null) {
            this.type = type
            with(binding) {
                when (type) {
                    Type.CREATED -> {
                        stepIcon.setImageResource(R.drawable.ic_tx_confirmations_start_16dp)
                        stepTitle.text = resources.getString(R.string.tx_confirmations_created)
                        stepTitle.setTextColor(ContextCompat.getColor(context, R.color.primary))
                    }
                    Type.CONFIRMED -> {
                        stepIcon.setImageResource(R.drawable.ic_tx_confirmations_done_16dp)
                        stepTitle.text = resources.getString(R.string.tx_confirmations_confirmed)
                        stepTitle.setTextColor(ContextCompat.getColor(context, R.color.primary))
                    }
                    Type.CANCELLED -> {
                        stepIcon.setImageResource(R.drawable.ic_tx_confirmations_canceled_16dp)
                        stepTitle.text = resources.getString(R.string.tx_confirmations_cancelled)
                        stepTitle.setTextColor(ContextCompat.getColor(context, R.color.label_secondary))
                    }
                    Type.FAILED -> {
                        stepIcon.setImageResource(R.drawable.ic_circle_cross_red_16dp)
                        stepTitle.text = resources.getString(R.string.tx_confirmations_failed)
                        stepTitle.setTextColor(ContextCompat.getColor(context, R.color.error))
                    }
                    Type.EXECUTE_WAITING -> {
                        stepIcon.setImageResource(R.drawable.ic_tx_confirmations_waiting_16dp)
                        stepTitle.text = resources.getString(R.string.tx_confirmations_execute_waiting, missingConfirmations)
                        stepTitle.setTextColor(ContextCompat.getColor(context, R.color.label_secondary))
                    }
                    Type.EXECUTE_READY -> {
                        stepIcon.setImageResource(R.drawable.ic_tx_confirmations_execute_ready_16dp)
                        stepTitle.text = resources.getString(R.string.tx_confirmations_execute_ready)
                        stepTitle.setTextColor(ContextCompat.getColor(context, R.color.label_primary))
                    }
                    Type.EXECUTE_DONE -> {
                        stepIcon.setImageResource(R.drawable.ic_tx_confirmations_done_16dp)
                        stepTitle.text = resources.getString(R.string.tx_confirmations_executed)
                        stepTitle.setTextColor(ContextCompat.getColor(context, R.color.primary))
                    }
                    Type.REJECTION_CREATED -> {
                        stepIcon.setImageResource(R.drawable.ic_circle_cross_red_16dp)
                        stepTitle.text = resources.getString(R.string.tx_confirmations_rejection_created)
                        stepTitle.setTextColor(ContextCompat.getColor(context, R.color.error))
                    }
                }
            }
        }
    }

    companion object {
        private const val ADDRESS_ITEM_HEIGHT = 44
        private const val ADDRESS_ITEM_MARGIN_LEFT = 24
        private const val EXECUTION_DESCRIPTION_ITEM_MARGIN_LEFT = 40
        private const val EXECUTION_DESCRIPTION_ITEM_MARGIN_RIGHT = 16
        private const val EXECUTION_DESCRIPTION_ITEM_MARGIN_VERTICAL = -10
        private const val MARGIN_VERTICAL = 16
        private const val LINE_WIDTH = 2
        private const val LINE_COLOR = R.color.label_tertiary
    }
}

