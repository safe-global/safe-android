package pm.gnosis.heimdall.ui.safe.pairing.connect

import android.content.Context
import android.content.Intent
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.safe.pairing.PairingStartActivity
import pm.gnosis.heimdall.ui.two_factor.Connect2FaActivity
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString

class Connect2FaStartActivity : PairingStartActivity() {

    override fun screenId() = ScreenId.CONNECT_2FA_START

    override fun titleRes(): Int = R.string.security_2fa

    override fun getImageRes(): Int = R.drawable.two_fa_setup

    override fun textRes(): Int = R.string.once_process_is_complete

    override fun onStartClicked() {
        startActivity(Connect2FaActivity.createIntent(this, safe))
    }

    companion object {

        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"

        fun createIntent(context: Context, safeAddress: Solidity.Address) = Intent(context, Connect2FaStartActivity::class.java).apply {
            putExtra(EXTRA_SAFE_ADDRESS, safeAddress.asEthereumAddressString())
        }
    }
}
