package pm.gnosis.android.app.authenticator.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import pm.gnosis.android.app.accounts.models.Transaction
import pm.gnosis.android.app.accounts.repositories.AccountsRepository
import pm.gnosis.android.app.authenticator.GnosisAuthenticatorApplication
import pm.gnosis.android.app.authenticator.R
import pm.gnosis.android.app.authenticator.di.component.DaggerViewComponent
import pm.gnosis.android.app.authenticator.di.module.ViewModule
import pm.gnosis.android.app.authenticator.ui.account.AccountFragment
import pm.gnosis.android.app.authenticator.ui.authenticate.AuthenticateFragment
import pm.gnosis.android.app.authenticator.ui.multisig.MultisigFragment
import pm.gnosis.android.app.authenticator.ui.tokens.TokensFragment
import pm.gnosis.android.app.authenticator.util.hexAsBigInteger
import java.math.BigInteger
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
