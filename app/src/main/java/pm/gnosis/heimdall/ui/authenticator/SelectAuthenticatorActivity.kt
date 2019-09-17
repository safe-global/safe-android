package pm.gnosis.heimdall.ui.authenticator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.layout_select_authenticator.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.keycard.KeycardIntroActivity
import pm.gnosis.heimdall.ui.safe.create.CreateSafePairingActivity
import pm.gnosis.heimdall.utils.AuthenticatorInfo

class SelectAuthenticatorActivity : BaseActivity() {

    private var selectedAuthenticator = AuthenticatorInfo.Type.KEYCARD

    override fun screenId() = ScreenId.SELECT_AUTHENTICATOR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_select_authenticator)

        select_authenticator_back_button.setOnClickListener { onBackPressed() }
        select_authenticator_keycard_background.setOnClickListener { onSelected(AuthenticatorInfo.Type.KEYCARD) }
        select_authenticator_extension_background.setOnClickListener { onSelected(AuthenticatorInfo.Type.EXTENSION) }
        select_authenticator_setup.setOnClickListener { startSetupForSelectedAuthenticator() }
    }

    private fun startSetupForSelectedAuthenticator() {
        val intent = when (selectedAuthenticator) {
            AuthenticatorInfo.Type.KEYCARD -> KeycardIntroActivity.createIntent(this)
            AuthenticatorInfo.Type.EXTENSION -> CreateSafePairingActivity.createIntent(this)
        }
        startActivityForResult(intent, AUTHENTICATOR_REQUEST_CODE)
    }

    private fun onSelected(type: AuthenticatorInfo.Type) {
        selectedAuthenticator = type
        select_authenticator_keycard_radio.isChecked = type == AuthenticatorInfo.Type.KEYCARD
        select_authenticator_extension_radio.isChecked = type == AuthenticatorInfo.Type.EXTENSION
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AUTHENTICATOR_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // Forward result
                setResult(Activity.RESULT_OK, data)
                finish()
            }
        } else
            super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val AUTHENTICATOR_REQUEST_CODE = 4242

        fun createIntent(context: Context) = Intent(context, SelectAuthenticatorActivity::class.java)
    }
}