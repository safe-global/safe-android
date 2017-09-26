package pm.gnosis.heimdall.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import kotlinx.android.synthetic.main.layout_main.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.account.AccountFragment
import pm.gnosis.heimdall.ui.authenticate.AuthenticateFragment
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.multisig.MultisigFragment
import pm.gnosis.heimdall.ui.tokens.TokensFragment

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main)

        layout_main_bottom_navigation.setOnNavigationItemSelectedListener {
            val fragment = when (it.itemId) {
                R.id.action_authenticate -> AuthenticateFragment()
                R.id.action_account -> AccountFragment()
                R.id.action_multisig -> MultisigFragment()
                R.id.action_tokens -> TokensFragment()
                else -> null
            }
            if (fragment != null) {
                replaceFragment(fragment)
                return@setOnNavigationItemSelectedListener true
            } else {
                return@setOnNavigationItemSelectedListener false
            }
        }

        layout_main_bottom_navigation.selectedItemId = R.id.action_authenticate
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.layout_main_content, fragment).commit()
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, MainActivity::class.java)
    }
}
