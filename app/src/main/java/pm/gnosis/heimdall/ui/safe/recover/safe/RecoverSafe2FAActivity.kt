package pm.gnosis.heimdall.ui.safe.recover.safe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.layout_select_authenticator.*
import pm.gnosis.heimdall.ui.two_factor.Select2FaActivity
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

class RecoverSafe2FAActivity : Select2FaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // allow to select Gnosis Authenticator when recovering for now
        select_authenticator_extension.visible(true)
        if (!nfcAvailable)
            onSelected(AuthenticatorInfo.Type.EXTENSION)
    }

    override fun onAuthenticatorSetupInfo(info: AuthenticatorSetupInfo) {
        val safeAddress = intent.getStringExtra(EXTRA_RECOVERING_SAFE)!!.asEthereumAddress()!!
        startActivity(RecoverSafeRecoveryPhraseActivity.createIntent(this, safeAddress, info))
    }

    companion object {
        private const val EXTRA_RECOVERING_SAFE = "extra.string.recovering_safe"

        fun createIntent(context: Context, recoveringSafe: Solidity.Address) =
            Intent(context, RecoverSafe2FAActivity::class.java)
                .addSelectAuthenticatorExtras(null)
                .apply {
                    putExtra(EXTRA_RECOVERING_SAFE, recoveringSafe.asEthereumAddressString())
                }
    }
}
