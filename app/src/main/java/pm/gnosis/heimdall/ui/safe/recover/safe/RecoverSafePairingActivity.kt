package pm.gnosis.heimdall.ui.safe.recover.safe

import android.content.Context
import android.content.Intent
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.safe.pairing.PairingActivity
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

class RecoverSafePairingActivity : PairingActivity() {
    override fun titleRes(): Int = R.string.recover_safe

    override fun onSuccess(extension: Solidity.Address) {
        val safeAddress = intent.getStringExtra(EXTRA_SAFE_ADDRESS).asEthereumAddress()!!
        startActivity(RecoverSafeRecoveryPhraseActivity.createIntent(this, safeAddress, extension))
    }

    override fun shouldShowLaterOption() = false

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"

        fun createIntent(context: Context, safeAddress: Solidity.Address) =
            Intent(context, RecoverSafePairingActivity::class.java).apply {
                putExtra(EXTRA_SAFE_ADDRESS, safeAddress.asEthereumAddressString())
            }
    }
}
