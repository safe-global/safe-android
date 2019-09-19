package pm.gnosis.heimdall.ui.authenticator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.layout_select_authenticator.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.keycard.KeycardIntroActivity
import pm.gnosis.heimdall.ui.safe.create.CreateSafePairingActivity
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.heimdall.utils.getAuthenticatorInfo
import pm.gnosis.heimdall.utils.put
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

@ExperimentalCoroutinesApi
open class SelectAuthenticatorActivity : BaseActivity() {

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

    protected fun getSelectAuthenticatorExtras(): Solidity.Address? = intent.getStringExtra(EXTRA_SAFE).asEthereumAddress()

    private fun startSetupForSelectedAuthenticator() {
        val intent = when (selectedAuthenticator) {
            AuthenticatorInfo.Type.KEYCARD -> KeycardIntroActivity.createIntent(this, getSelectAuthenticatorExtras())
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
                data.getAuthenticatorInfo()?.let { onAuthenticatorSetupInfo(it) }
            }
        } else
            super.onActivityResult(requestCode, resultCode, data)
    }

    open fun onAuthenticatorSetupInfo(info: AuthenticatorSetupInfo) {
        setResult(Activity.RESULT_OK, info.put(Intent()))
        finish()
    }

    companion object {
        private const val AUTHENTICATOR_REQUEST_CODE = 4242

        fun Intent.addSelectAuthenticatorExtras(safe: Solidity.Address?): Intent = apply {
            putExtra(EXTRA_SAFE, safe?.asEthereumAddressString())
        }

        private const val EXTRA_SAFE = "extra.string.safe"
        fun createIntent(context: Context, safe: Solidity.Address?) =
            Intent(context, SelectAuthenticatorActivity::class.java).addSelectAuthenticatorExtras(safe)
    }
}