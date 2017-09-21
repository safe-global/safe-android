package pm.gnosis.heimdall.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import pm.gnosis.heimdall.accounts.repositories.AccountsRepository
import pm.gnosis.heimdall.GnosisAuthenticatorApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.component.DaggerViewComponent
import pm.gnosis.heimdall.common.di.module.ViewModule
import pm.gnosis.heimdall.ui.account.AccountFragment
import pm.gnosis.heimdall.ui.authenticate.AuthenticateFragment
import pm.gnosis.heimdall.ui.multisig.MultisigFragment
import pm.gnosis.heimdall.ui.tokens.TokensFragment
import javax.inject.Inject

class MainActivity : AppCompatActivity() {


    @Inject
    lateinit var accountsRepository: AccountsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.activity_main)

        bottom_navigation.setOnNavigationItemSelectedListener {
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

        bottom_navigation.selectedItemId = R.id.action_authenticate
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.main_content, fragment).commit()
    }

    fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(GnosisAuthenticatorApplication[this].component)
                .viewModule(ViewModule(this))
                .build().inject(this)
    }
}
