package pm.gnosis.heimdall.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.layout_settings.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.account.AccountActivity
import pm.gnosis.heimdall.ui.addressbook.list.AddressBookActivity
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.settings.network.NetworkSettingsActivity
import pm.gnosis.heimdall.ui.settings.security.SecuritySettingsActivity
import pm.gnosis.heimdall.ui.settings.tokens.TokenManagementActivity

class SettingsActivity : BaseActivity() {

    override fun screenId() = ScreenId.SETTINGS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_settings)
        registerToolbar(layout_settings_toolbar)

        layout_settings_address_book.setOnClickListener {
            startActivity(AddressBookActivity.createIntent(this))
        }

        layout_settings_account.setOnClickListener {
            startActivity(AccountActivity.createIntent(this))
        }

        layout_settings_network.setOnClickListener {
            startActivity(NetworkSettingsActivity.createIntent(this))
        }

        layout_settings_tokens.setOnClickListener {
            startActivity(TokenManagementActivity.createIntent(this))
        }

        if (encryptionManager.canSetupFingerprint()) {
            layout_settings_security.setOnClickListener {
                startActivity(SecuritySettingsActivity.createIntent(this))
            }
        }
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, SettingsActivity::class.java)
    }
}
