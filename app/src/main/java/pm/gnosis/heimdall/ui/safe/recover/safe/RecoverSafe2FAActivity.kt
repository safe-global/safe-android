package pm.gnosis.heimdall.ui.safe.recover.safe

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pm.gnosis.heimdall.ui.two_factor.Select2FaActivity
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

@ExperimentalCoroutinesApi
class RecoverSafe2FAActivity : Select2FaActivity() {

    override fun onAuthenticatorSetupInfo(info: AuthenticatorSetupInfo) {
        val safeAddress = intent.getStringExtra(EXTRA_SAFE)!!.asEthereumAddress()!!
        startActivity(RecoverSafeRecoveryPhraseActivity.createIntent(this, safeAddress, info))
    }

    companion object {
        private const val EXTRA_SAFE = "extra.string.safe"

        fun createIntent(context: Context, recoveringSafe: Solidity.Address) =
            Intent(context, RecoverSafe2FAActivity::class.java)
                .addSelectAuthenticatorExtras(null)
                .apply {
                    putExtra(EXTRA_SAFE, recoveringSafe.asEthereumAddressString())
                }
    }
}
