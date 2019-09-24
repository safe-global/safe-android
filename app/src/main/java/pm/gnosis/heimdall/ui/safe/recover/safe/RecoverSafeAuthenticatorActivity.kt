package pm.gnosis.heimdall.ui.safe.recover.safe

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pm.gnosis.heimdall.ui.authenticator.SelectAuthenticatorActivity
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

@ExperimentalCoroutinesApi
class RecoverSafeAuthenticatorActivity : SelectAuthenticatorActivity() {

    override fun onAuthenticatorSetupInfo(info: AuthenticatorSetupInfo) {
        val safeAddress = intent.getStringExtra(EXTRA_RECOVERING_SAFE)!!.asEthereumAddress()!!
        startActivity(RecoverSafeRecoveryPhraseActivity.createIntent(this, safeAddress, info))
    }

    companion object {
        private const val EXTRA_RECOVERING_SAFE = "extra.string.recovering_safe"

        fun createIntent(context: Context, recoveringSafe: Solidity.Address) =
            Intent(context, RecoverSafeAuthenticatorActivity::class.java)
                .addSelectAuthenticatorExtras(null)
                .apply {
                    putExtra(EXTRA_RECOVERING_SAFE, recoveringSafe.asEthereumAddressString())
                }
    }
}
