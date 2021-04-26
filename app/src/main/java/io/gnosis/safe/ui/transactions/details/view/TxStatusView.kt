package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import com.google.android.material.shape.ShapeAppearanceModel
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewTxStatusBinding
import pm.gnosis.svalinn.common.utils.visible

class TxStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewTxStatusBinding.inflate(LayoutInflater.from(context), this) }

    fun setStatus(
        title: String,
        iconUrl: String? = null,
        @DrawableRes defaultIconRes: Int = R.drawable.ic_code_16dp,
        @StringRes statusTextRes: Int,
        @ColorRes statusColorRes: Int,
        safeApp: Boolean = false
    ) {

        binding.statusTitle.text = title
        iconUrl?.let {
            binding.typeIcon.loadKnownSafeAppLogo(logoUri = iconUrl, defaultResId = defaultIconRes)
            binding.typeIcon.shapeAppearanceModel = ShapeAppearanceModel.Builder().setAllCornerSizes(0.5F).build()
        } ?: binding.typeIcon.setImageResource(defaultIconRes)

        binding.appLabel.visible(safeApp)

        if (statusTextRes == R.string.tx_status_needs_confirmations ||
            statusTextRes == R.string.tx_status_needs_your_confirmation ||
            statusTextRes == R.string.tx_status_needs_execution
        ) {
            binding.statusLong.setText(statusTextRes)
            binding.statusLong.setTextColor(ContextCompat.getColor(context, statusColorRes))
            binding.statusLong.visible(true)
            binding.status.visible(false, View.INVISIBLE)
            adjustStatusTitleWidth(safeApp, true)
        } else {
            binding.status.setText(statusTextRes)
            binding.status.setTextColor(ContextCompat.getColor(context, statusColorRes))
            binding.status.visible(true)
            binding.statusLong.visible(false)
            adjustStatusTitleWidth(safeApp, false)
        }
    }

    private fun adjustStatusTitleWidth(appLabelVisible: Boolean, longStatusVisible: Boolean) {
        with(binding) {
            val bindingAsView = binding.root
            val viewTreeObserver: ViewTreeObserver = bindingAsView.viewTreeObserver
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        bindingAsView.viewTreeObserver.removeGlobalOnLayoutListener(this)
                        val viewWidth = bindingAsView.width
                        val statusTitleTextBounds = android.graphics.Rect()
                        statusTitle.paint.getTextBounds(statusTitle.text.toString(), 0, statusTitle.text.length, statusTitleTextBounds)
                        val statusTitleWidth = statusTitleTextBounds.right - statusTitleTextBounds.left

                        var statusWidth = 0
                        if (!longStatusVisible) {
                            val statusTextBounds = android.graphics.Rect()
                            status.paint.getTextBounds(status.text.toString(), 0, status.text.length, statusTextBounds)
                            val statusTextWidth = statusTextBounds.right - statusTextBounds.left
                            statusWidth = statusTextWidth + status.marginStart + status.marginEnd
                        }
                        var appLabelWidth = 0
                        if (appLabelVisible) {
                            val appLabelTextBounds = android.graphics.Rect()
                            appLabel.paint.getTextBounds(appLabel.text.toString(), 0, appLabel.text.length, appLabelTextBounds)
                            appLabelWidth = appLabelTextBounds.right - appLabelTextBounds.left + appLabel.marginStart + appLabel.marginEnd
                        }
                        val typeIconWidth = typeIcon.width + typeIcon.marginStart + typeIcon.marginEnd + typeIcon.marginStart

                        if (statusTitleWidth > viewWidth - appLabelWidth - statusWidth - typeIconWidth) {
                            statusTitle.width = viewWidth - appLabelWidth - statusWidth - typeIconWidth
                            statusTitle.ellipsize = android.text.TextUtils.TruncateAt.END
                        } else {
                            statusTitle.ellipsize = null
                            statusTitle.width = statusTitleWidth
                        }
                    }
                })
            }
        }
    }

    fun setStatus(
        @StringRes titleRes: Int,
        @DrawableRes iconRes: Int,
        @StringRes statusTextRes: Int,
        @ColorRes statusColorRes: Int
    ) {
        val resources = context.resources
        setStatus(title = resources.getString(titleRes), defaultIconRes = iconRes, statusTextRes = statusTextRes, statusColorRes = statusColorRes)
    }
}
