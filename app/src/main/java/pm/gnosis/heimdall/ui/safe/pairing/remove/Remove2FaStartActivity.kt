package pm.gnosis.heimdall.ui.safe.pairing.remove

import android.content.Context
import android.content.Intent
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.safe.pairing.PairingStartActivity
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString

class Remove2FaStartActivity : PairingStartActivity() {

    override fun screenId() = ScreenId.REMOVE_2FA_START

    override fun titleRes(): Int = R.string.disable_2fa

    override fun getImageRes(): Int = R.drawable.img_2fa_disable

    override fun textRes(): Int = R.string.disable_2fa_description

    override fun onStartClicked() {
        viewModel.loadAuthenticatorInfo()
    }

    override fun onAuthenticatorInfoLoaded(info: AuthenticatorSetupInfo) {
        startActivity(Remove2FaRecoveryPhraseActivity.createIntent(this, safe, info))
    }

    companion object {

        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"

        fun createIntent(context: Context, safeAddress: Solidity.Address) = Intent(context, Remove2FaStartActivity::class.java).apply {
            putExtra(EXTRA_SAFE_ADDRESS, safeAddress.asEthereumAddressString())
        }
    }
}
