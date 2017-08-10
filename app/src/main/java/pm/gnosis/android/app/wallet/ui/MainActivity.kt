package pm.gnosis.android.app.wallet.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import pm.gnosis.android.app.wallet.GnosisApplication
import pm.gnosis.android.app.wallet.R
import pm.gnosis.android.app.wallet.di.component.DaggerViewComponent
import pm.gnosis.android.app.wallet.di.module.ViewModule
import pm.gnosis.android.app.wallet.ui.account.AccountFragment
import pm.gnosis.android.app.wallet.ui.multisig.MultisigFragment
import pm.gnosis.android.app.wallet.ui.scan.ScanFragment
import pm.gnosis.android.app.wallet.ui.tokens.TokensFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.activity_main)

        bottom_navigation.setOnNavigationItemSelectedListener {
            val fragment = when (it.itemId) {
                R.id.action_account -> AccountFragment()
                R.id.action_multisig -> MultisigFragment()
                R.id.action_tokens -> TokensFragment()
                R.id.action_scan -> ScanFragment()
                else -> null
            }
            if (fragment != null) {
                replaceFragment(fragment)
                return@setOnNavigationItemSelectedListener true
            } else {
                return@setOnNavigationItemSelectedListener false
            }
        }

        bottom_navigation.selectedItemId = R.id.action_account
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.main_content, fragment).commit()
    }

    fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(GnosisApplication[this].component)
                .viewModule(ViewModule(this))
                .build().inject(this)
    }
}
