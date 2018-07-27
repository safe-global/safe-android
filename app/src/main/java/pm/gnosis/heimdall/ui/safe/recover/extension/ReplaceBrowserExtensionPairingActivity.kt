package pm.gnosis.heimdall.ui.safe.recover.extension

import android.content.Context
import android.content.Intent
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.safe.pairing.PairingActivity
import pm.gnosis.heimdall.ui.safe.recover.phrase.ReplaceBrowserExtensionRecoveryPhraseActivity
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

class ReplaceBrowserExtensionPairingActivity : PairingActivity() {
    override fun titleRes() = R.string.replace_browser_extension

    override fun onSuccess(extension: Solidity.Address) {
        val safeAddress = intent.getStringExtra(ReplaceBrowserExtensionPairingActivity.EXTRA_SAFE_ADDRESS).asEthereumAddress()!!
        startActivity(ReplaceBrowserExtensionRecoveryPhraseActivity.createIntent(this, safeAddress, extension))
    }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"

        fun createIntent(context: Context, safeAddress: Solidity.Address) =
            Intent(context, ReplaceBrowserExtensionPairingActivity::class.java).apply {
                putExtra(EXTRA_SAFE_ADDRESS, safeAddress.asEthereumAddressString())
            }
    }
}
