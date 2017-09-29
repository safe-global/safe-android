package pm.gnosis.heimdall.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.IdRes
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.util.SparseArray
import kotlinx.android.synthetic.main.layout_main.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.account.AccountFragment
import pm.gnosis.heimdall.ui.authenticate.AuthenticateFragment
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.multisig.MultisigFragment
import pm.gnosis.heimdall.ui.tokens.TokensFragment

class MainActivity : BaseActivity() {

    val items = listOf(R.id.action_authenticate, R.id.action_account, R.id.action_multisig, R.id.action_tokens)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main)

        layout_main_toolbar.setTitle(R.string.app_name)
        layout_main_viewpager.adapter = pagerAdapter()
        layout_main_viewpager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                layout_main_bottom_navigation.selectedItemId = positionToId(position)
            }
        })
        layout_main_bottom_navigation.setOnNavigationItemSelectedListener {
            layout_main_viewpager.currentItem = idToPosition(it.itemId)
            return@setOnNavigationItemSelectedListener true
        }

        layout_main_bottom_navigation.selectedItemId = R.id.action_authenticate
    }

    private fun idToPosition(@IdRes itemId: Int) = items.indexOf(itemId)

    private fun positionToId(position: Int) = items.getOrElse(position, { -1 })

    private fun pagerAdapter() = object : FragmentPagerAdapter(supportFragmentManager) {
        private val fragments: SparseArray<Fragment> = SparseArray()

        override fun getItem(position: Int): Fragment {
            var fragment = fragments.get(position)
            if (fragment == null) {
                fragment = createFragment(position)
                fragments.put(position, fragment)
            }
            return fragment
        }

        private fun createFragment(position: Int) =
                when (positionToId(position)) {
                    R.id.action_authenticate -> {
                        AuthenticateFragment()
                    }
                    R.id.action_account -> {
                        AccountFragment()
                    }
                    R.id.action_multisig -> {
                        MultisigFragment()
                    }
                    R.id.action_tokens -> {
                        TokensFragment()
                    }
                    else -> throw IllegalStateException("Unhandled tab position")
                }

        override fun getCount() = items.size
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, MainActivity::class.java)
    }
}
