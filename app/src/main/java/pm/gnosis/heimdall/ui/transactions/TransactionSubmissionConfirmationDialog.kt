package pm.gnosis.heimdall.ui.transactions

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.ViewGroup
import kotlinx.android.synthetic.main.layout_safe_transaction_confirmation.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.dialogs.base.BaseDialog
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.svalinn.common.utils.getColorCompat


class TransactionSubmissionConfirmationDialog : BaseDialog() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FullScreenDialogStyle)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_safe_transaction_confirmation, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            with(dialog) {
                window?.statusBarColor = context.getColorCompat(R.color.white)
                window?.decorView?.systemUiVisibility = SYSTEM_UI_FLAG_LAYOUT_STABLE

                if (Build.VERSION.SDK_INT >= 26) {
                    window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else if (Build.VERSION.SDK_INT >= 23) {
                    window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            }
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()
        congratulations_continue.setOnClickListener {
            startActivity(
                SafeMainActivity.createIntent(
                    context!!,
                    null,
                    R.string.tab_title_transactions
                )
            )
            dismiss()
        }
    }

    companion object {
        fun create() = TransactionSubmissionConfirmationDialog()
    }
}