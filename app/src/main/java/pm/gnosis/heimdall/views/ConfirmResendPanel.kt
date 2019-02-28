package pm.gnosis.heimdall.views

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DrawableRes
import androidx.cardview.widget.CardView
import pm.gnosis.heimdall.R
import kotlinx.android.synthetic.main.view_confirm_resend_panel.view.confirm_reject_panel_action_primary as actionPrimaryBtn
import kotlinx.android.synthetic.main.view_confirm_resend_panel.view.confirm_reject_panel_action_secondary as actionSecondaryBtn
import kotlinx.android.synthetic.main.view_confirm_resend_panel.view.confirm_reject_panel_title as titleLabel
import kotlinx.android.synthetic.main.view_confirm_resend_panel.view.confirm_reject_panel_message as messageLabel
import kotlinx.android.synthetic.main.view_confirm_resend_panel.view.confirm_reject_panel_image as statusImg


class ConfirmResendPanel @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    @DrawableRes
    var image: Int = 0
        set(value) {
            field = value
            statusImg.setImageResource(value)
        }

    var title: String? = null
        set(value) {
            field = value
            titleLabel.text = value
        }

    var message: String? = null
        set(value) {
            field = value
            messageLabel.text = value
        }

    var actionTitle: String? = null
        set(value) {
            field = value
            setupActions(value ?: "", actionPrimary)
        }

    var actionPrimary: Boolean = true
        set(value) {
            field = value
            setupActions(actionTitle ?: "", value)
        }


    var action: ((View) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_confirm_resend_panel, this, true)
        readAttributesAndSetupFields(context, attrs)

        actionPrimaryBtn.setOnClickListener {
            action?.invoke(it)
        }
        actionSecondaryBtn.setOnClickListener {
            action?.invoke(it)
        }
    }

    private fun readAttributesAndSetupFields(context: Context, attrs: AttributeSet?) {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ConfirmResendPanel,
            0, 0
        )
        try {
            applyAttributes(context, a)
        } finally {
            a.recycle()
        }
    }

    private fun applyAttributes(context: Context, a: TypedArray) {

        title = a.getString(R.styleable.ConfirmResendPanel_actionTitle)
        message = a.getString(R.styleable.ConfirmResendPanel_message)
        actionTitle = a.getString(R.styleable.ConfirmResendPanel_actionTitle)
        actionPrimary = a.getBoolean(R.styleable.ConfirmResendPanel_actionPrimary, true)
        image = a.getResourceId(R.styleable.ConfirmResendPanel_image, 0)

        setupActions(actionTitle ?: "", actionPrimary)

        titleLabel.text = title
        messageLabel.text = message
        statusImg.setImageResource(image)
    }

    private fun setupActions(title: String, primary: Boolean) {
        if (primary) {
            actionPrimaryBtn.visibility = View.VISIBLE
            actionPrimaryBtn.text = title
            actionSecondaryBtn.visibility = View.GONE
        } else {
            actionPrimaryBtn.visibility = View.GONE
            actionSecondaryBtn.visibility = View.VISIBLE
            actionSecondaryBtn.text = title
        }
    }
}