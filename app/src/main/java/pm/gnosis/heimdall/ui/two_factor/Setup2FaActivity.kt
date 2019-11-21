package pm.gnosis.heimdall.ui.two_factor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.layout_setup_authenticator.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.helpers.NfcActivity
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.safe.create.CreateSafeRecoveryPhraseIntroActivity
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.heimdall.utils.getAuthenticatorInfo
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

class Setup2FaActivity : NfcActivity() {
    override fun screenId() = ScreenId.SETUP_AUTHENTICATOR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_setup_authenticator)

        setup_authenticator_back_button.setOnClickListener { onBackPressed() }
        setup_authenticator_skip.setOnClickListener { onAuthenticatorSelected(null) }
        setup_authenticator_setup.setOnClickListener {
            startActivityForResult(
                Select2FaActivity.createOnboardingIntent(this, intent.getStringExtra(EXTRA_SAFE)?.asEthereumAddress()),
                AUTHENTICATOR_REQUEST_CODE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AUTHENTICATOR_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                onAuthenticatorSelected(data.getAuthenticatorInfo())
            }
        } else
            super.onActivityResult(requestCode, resultCode, data)
    }

    private fun onAuthenticatorSelected(authenticatorInfo: AuthenticatorSetupInfo?) {
        startActivity(CreateSafeRecoveryPhraseIntroActivity.createIntent(this, authenticatorInfo))
    }

    companion object {
        private const val AUTHENTICATOR_REQUEST_CODE = 4242

        private const val EXTRA_SAFE = "extra.string.safe"
        fun createIntent(context: Context, safe: Solidity.Address?) =
            Intent(context, Setup2FaActivity::class.java).apply {
                putExtra(EXTRA_SAFE, safe?.asEthereumAddressString())
            }
    }
}
