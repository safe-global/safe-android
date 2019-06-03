package pm.gnosis.heimdall.ui.transactions

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import kotlinx.android.synthetic.main.layout_safe_transaction_confirmation.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.svalinn.common.utils.getColorCompat


class TransactionSubmissionConfirmationDialog(context: Context) : Dialog(context, R.style.FullScreenDialogStyle) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            window.statusBarColor = context.getColorCompat(R.color.white)
            window.decorView.systemUiVisibility = SYSTEM_UI_FLAG_LAYOUT_STABLE

            if (Build.VERSION.SDK_INT >= 26) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else if (Build.VERSION.SDK_INT >= 23) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }

        setContentView(R.layout.layout_safe_transaction_confirmation)

        congratulations_continue.setOnClickListener {
            context.startActivity(
                SafeMainActivity.createIntent(
                    context,
                    null,
                    R.string.tab_title_transactions
                )
            )
            dismiss()
        }
    }
}