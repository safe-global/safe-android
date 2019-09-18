package pm.gnosis.heimdall.ui.keycard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.layout_keycard_intro.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.heimdall.utils.put

@ExperimentalCoroutinesApi
class KeycardIntroActivity : BaseActivity(), KeycardPairingDialog.PairingCallback {

    override fun screenId() = ScreenId.KEYCARD_INTRO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_keycard_intro)

        keycard_intro_back_button.setOnClickListener { onBackPressed() }
        keycard_intro_setup.setOnClickListener {
            KeycardPairingDialog().show(supportFragmentManager, null)
        }
    }
    override fun onPaired(authenticatorInfo: AuthenticatorSetupInfo) {
        setResult(Activity.RESULT_OK, authenticatorInfo.put(Intent()))
        finish()
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, KeycardIntroActivity::class.java)
    }

}