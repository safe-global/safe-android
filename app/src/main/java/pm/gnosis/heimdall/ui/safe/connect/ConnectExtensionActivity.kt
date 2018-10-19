package pm.gnosis.heimdall.ui.safe.connect

import android.content.Context
import android.content.Intent
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.ui.safe.pairing.PairingActivity
import pm.gnosis.heimdall.ui.transactions.view.review.ReviewTransactionActivity
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

class ConnectExtensionActivity : PairingActivity() {
    override fun titleRes() = R.string.connect

    override fun onSuccess(extension: Solidity.Address) {
        startActivity(
            ReviewTransactionActivity.createIntent(
                context = this,
                safe = intent.getStringExtra(EXTRA_SAFE_ADDRESS).asEthereumAddress()!!,
                txData = TransactionData.ConnectExtension(extension)
            )
        )
    }

    override fun shouldShowSkip() = false

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"

        fun createIntent(context: Context, safeAddress: Solidity.Address) = Intent(context, ConnectExtensionActivity::class.java).apply {
            putExtra(EXTRA_SAFE_ADDRESS, safeAddress.asEthereumAddressString())
        }
    }
}
