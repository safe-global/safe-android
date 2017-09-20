package pm.gnosis.heimdall.ui.authenticate

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_authenticate.*
import pm.gnosis.heimdall.MultiSigWalletWithDailyLimit
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.component.ApplicationComponent
import pm.gnosis.heimdall.di.component.DaggerViewComponent
import pm.gnosis.heimdall.di.module.ViewModule
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.transactiondetails.TransactionDetailsActivity
import pm.gnosis.heimdall.util.ERC67Parser
import pm.gnosis.heimdall.util.scanQrCode
import pm.gnosis.heimdall.util.snackbar
import pm.gnosis.heimdall.util.zxing.ZxingIntentIntegrator
import pm.gnosis.utils.isSolidityMethod

class AuthenticateFragment : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater?.inflate(R.layout.fragment_authenticate, container, false)

    override fun onStart() {
        super.onStart()
        fragment_authenticate_scan.setOnClickListener {
            scanQrCode()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ZxingIntentIntegrator.REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA)) {
                validateQrCode(data.getStringExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA))
            } else if (resultCode == Activity.RESULT_CANCELED) {
                snackbar(fragment_scan_coordinator, "Cancelled by the user")
            }
        }
    }

    private fun validateQrCode(qrCodeData: String) {
        val transaction = ERC67Parser.parse(qrCodeData)
        if (transaction != null) {
            val data = transaction.data
            if (data != null && (data.isSolidityMethod(MultiSigWalletWithDailyLimit.ConfirmTransaction.METHOD_ID) ||
                    data.isSolidityMethod(MultiSigWalletWithDailyLimit.RevokeConfirmation.METHOD_ID))) {
                val intent = Intent(context, TransactionDetailsActivity::class.java)
                intent.putExtra(TransactionDetailsActivity.TRANSACTION_EXTRA, transaction)
                startActivity(intent)
            } else {
                snackbar(fragment_scan_coordinator, "Not Confirm or Revoke")
            }
        } else {
            snackbar(fragment_scan_coordinator, "Not a valid ERC67 transaction")
        }
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
                .applicationComponent(component)
                .viewModule(ViewModule(this.context))
                .build().inject(this)
    }
}
