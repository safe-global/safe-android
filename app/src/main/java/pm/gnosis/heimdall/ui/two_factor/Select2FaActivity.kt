package pm.gnosis.heimdall.ui.two_factor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.layout_nfc_required.*
import kotlinx.android.synthetic.main.layout_select_authenticator.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.helpers.NfcActivity
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.settings.general.GetInTouchActivity
import pm.gnosis.heimdall.ui.two_factor.authenticator.PairingAuthenticatorActivity
import pm.gnosis.heimdall.ui.two_factor.keycard.KeycardIntroActivity
import pm.gnosis.heimdall.utils.*
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString


open class Select2FaActivity : NfcActivity() {

    private var selectedAuthenticator: AuthenticatorInfo.Type? = null
    protected var nfcAvailable = false

    override fun screenId() = ScreenId.SELECT_AUTHENTICATOR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_select_authenticator)

        select_authenticator_back_button.setOnClickListener { onBackPressed() }
        select_authenticator_extension_background.setOnClickListener { onSelected(AuthenticatorInfo.Type.EXTENSION) }
        select_authenticator_setup.setOnClickListener { startSetupForSelectedAuthenticator() }
        initKeyCardViews()
    }

    private fun initKeyCardViews() {
        nfcAvailable = NfcAdapter.getDefaultAdapter(this) != null
        if (nfcAvailable) {
            onSelected(AuthenticatorInfo.Type.KEYCARD)
            select_authenticator_keycard_background.setOnClickListener { onSelected(AuthenticatorInfo.Type.KEYCARD) }
        } else {
            onSelected(null)
            select_authenticator_nfc_required.visible(true)
            select_authenticator_keycard_nfc.visible(true)
            nfc_required_get_in_touch.setOnClickListener {
                startActivity(GetInTouchActivity.newIntent(this))
            }
        }

        select_authenticator_keycard_description.apply {
            val linkDrawable = ContextCompat.getDrawable(context, R.drawable.ic_external_link)!!
            linkDrawable.setBounds(0, 0, linkDrawable.intrinsicWidth, linkDrawable.intrinsicHeight)
            val spannable = SpannableString(getString(R.string.status_keycard_description))
            spannable.setSpan(
                ForegroundColorSpan(getColorCompat(if (nfcAvailable) R.color.dark_grey else R.color.dark_grey_disabled)),
                0,
                text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            text = spannable
            appendLink(getString(R.string.keycard_link), getString(R.string.learn_more))
        }

        val alpha = if (nfcAvailable) 1f else 0.6f
        select_authenticator_keycard_background.isClickable = nfcAvailable
        select_authenticator_keycard_background.alpha = alpha
        select_authenticator_keycard_radio.alpha = alpha
        select_authenticator_keycard_img.alpha = alpha
        select_authenticator_keycard_lbl.alpha = alpha
        select_authenticator_keycard_nfc.alpha = alpha
    }

    protected fun getSelectAuthenticatorExtras(): Solidity.Address? = intent.getStringExtra(EXTRA_SAFE)?.asEthereumAddress()

    protected fun isOnboarding(): Boolean = intent.getBooleanExtra(EXTRA_ONBOARDING, false)

    private fun startSetupForSelectedAuthenticator() {

        selectedAuthenticator?.let {
            val intent = when (it) {
                AuthenticatorInfo.Type.KEYCARD -> KeycardIntroActivity.createIntent(this, getSelectAuthenticatorExtras(), isOnboarding())
                AuthenticatorInfo.Type.EXTENSION -> PairingAuthenticatorActivity.createIntent(this, getSelectAuthenticatorExtras(), isOnboarding())
            }
            startActivityForResult(intent, AUTHENTICATOR_REQUEST_CODE)
        }
    }

    protected fun onSelected(type: AuthenticatorInfo.Type?) {
        selectedAuthenticator = type
        select_authenticator_keycard_radio.isChecked = type == AuthenticatorInfo.Type.KEYCARD
        select_authenticator_extension_radio.isChecked = type == AuthenticatorInfo.Type.EXTENSION

        select_authenticator_setup.isEnabled = type == AuthenticatorInfo.Type.EXTENSION || nfcAvailable
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

        private const val EXTRA_SAFE = "extra.string.safe"
        private const val EXTRA_ONBOARDING = "extra.boolean.onboarding"

        fun Intent.addSelectAuthenticatorExtras(safe: Solidity.Address?, onboarding: Boolean = false): Intent = apply {
            putExtra(EXTRA_SAFE, safe?.asEthereumAddressString())
            putExtra(EXTRA_ONBOARDING, onboarding)
        }

        fun createIntent(context: Context, safe: Solidity.Address?) =
            Intent(context, Select2FaActivity::class.java).addSelectAuthenticatorExtras(safe)

        fun createOnboardingIntent(context: Context, safe: Solidity.Address?) =
            Intent(context, Select2FaActivity::class.java).addSelectAuthenticatorExtras(safe, true)
    }
}
