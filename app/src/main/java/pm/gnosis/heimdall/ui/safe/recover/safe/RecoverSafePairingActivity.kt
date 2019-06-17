package pm.gnosis.heimdall.ui.safe.recover.safe

import android.content.Context
import android.content.Intent
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.ui.safe.connect.ConnectExtensionActivity
import pm.gnosis.heimdall.ui.safe.pairing.PairingActivity
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

class RecoverSafePairingActivity : PairingActivity() {
    override fun titleRes(): Int = R.string.recover_safe

    override fun onSuccess(signingOwner: AccountsRepository.SafeOwner, extension: Solidity.Address) {
        val safeAddress = intent.getStringExtra(EXTRA_SAFE_ADDRESS).asEthereumAddress()!!
        startActivity(RecoverSafeRecoveryPhraseActivity.createIntent(this, safeAddress, extension, signingOwner))
    }

    override fun shouldShowLaterOption() = false

    // Since we recover the Safe we want to create a new signing key
    override fun signingSafe(): Solidity.Address? = null

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"

        fun createIntent(context: Context, safeAddress: Solidity.Address) =
            Intent(context, RecoverSafePairingActivity::class.java).apply {
                putExtra(EXTRA_SAFE_ADDRESS, safeAddress.asEthereumAddressString())
            }
    }
}
