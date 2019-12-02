package pm.gnosis.heimdall.ui.two_factor.keycard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.layout_keycard_intro.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.helpers.NfcActivity
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.heimdall.utils.put
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

@ExperimentalCoroutinesApi
class KeycardIntroActivity : NfcActivity(), KeycardPairingDialog.PairingCallback {

    override fun screenId() = ScreenId.KEYCARD_INTRO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_keycard_intro)

        step_indicator.visible(intent.getBooleanExtra(EXTRA_ONBOARDING, false))

        keycard_intro_back_button.setOnClickListener { onBackPressed() }
        keycard_intro_setup.setOnClickListener {
            KeycardPairingDialog.create(intent.getStringExtra(EXTRA_SAFE)?.asEthereumAddress()).show(supportFragmentManager, null)
        }
    }
    override fun onPaired(authenticatorInfo: AuthenticatorSetupInfo) {
        setResult(Activity.RESULT_OK, authenticatorInfo.put(Intent()))
        finish()
    }

    companion object {
        private const val EXTRA_SAFE = "extra.string.safe"
        private const val EXTRA_ONBOARDING = "extra.boolean.onboarding"

        fun createIntent(context: Context, safe: Solidity.Address?, onboarding: Boolean = false) =
            Intent(context, KeycardIntroActivity::class.java).apply {
                putExtra(EXTRA_SAFE, safe?.asEthereumAddressString())
                putExtra(EXTRA_ONBOARDING, onboarding)
            }
    }

}
