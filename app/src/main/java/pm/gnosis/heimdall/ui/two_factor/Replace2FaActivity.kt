package pm.gnosis.heimdall.ui.two_factor

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pm.gnosis.heimdall.ui.safe.pairing.replace.Replace2FaRecoveryPhraseActivity
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity

class Replace2FaActivity : Select2FaActivity() {

    override fun onAuthenticatorSetupInfo(info: AuthenticatorSetupInfo) {
        val safeAddress = getSelectAuthenticatorExtras()!!
        startActivity(
            Replace2FaRecoveryPhraseActivity.createIntent(
                this,
                safeAddress,
                info
            )
        )
    }

    companion object {
        fun createIntent(context: Context, safe: Solidity.Address) =
            Intent(context, Replace2FaActivity::class.java).addSelectAuthenticatorExtras(safe)
    }
}
