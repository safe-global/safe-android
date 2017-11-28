package pm.gnosis.heimdall.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.IdRes
import android.support.v4.view.ViewPager
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.layout_main.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.account.AccountFragment
import pm.gnosis.heimdall.ui.addressbook.list.AddressBookActivity
import pm.gnosis.heimdall.ui.authenticate.AuthenticateFragment
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.base.FactoryPagerAdapter
import pm.gnosis.heimdall.ui.safe.overview.SafeOverviewFragment
import pm.gnosis.heimdall.ui.settings.SettingsActivity
import pm.gnosis.heimdall.ui.tokens.overview.TokensFragment

class MainActivity : BaseActivity() {

    private val items = listOf(R.id.action_authenticate, R.id.action_account, R.id.action_safe, R.id.action_tokens)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main)

        layout_main_toolbar.setTitle(R.string.app_name)
        layout_main_toolbar.inflateMenu(R.menu.main_menu)
        layout_main_toolbar.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.main_menu_settings -> startActivity(SettingsActivity.createIntent(this))
                R.id.main_menu_address_book -> startActivity(AddressBookActivity.createIntent(this))
            }
            true
        }
        layout_main_viewpager.adapter = pagerAdapter()
        layout_main_viewpager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                layout_main_bottom_navigation.selectedItemId = positionToId(position)
            }
        })
        layout_main_bottom_navigation.setOnNavigationItemSelectedListener {
            layout_main_viewpager.currentItem = idToPosition(it.itemId)
            true
        }

        layout_main_bottom_navigation.selectedItemId = R.id.action_authenticate
    }

    private fun idToPosition(@IdRes itemId: Int) = items.indexOf(itemId)

    private fun positionToId(position: Int) = items.getOrElse(position, { -1 })

    private fun pagerAdapter() = FactoryPagerAdapter(supportFragmentManager, FactoryPagerAdapter.Factory(items.size, {
        when (positionToId(it)) {
            R.id.action_authenticate -> {
                AuthenticateFragment()
            }
            R.id.action_account -> {
                AccountFragment()
            }
            R.id.action_safe -> {
                SafeOverviewFragment()
            }
            R.id.action_tokens -> {
                TokensFragment()
            }
            else -> throw IllegalStateException("Unhandled tab position")
        }
    }))

    companion object {
        fun createIntent(context: Context) = Intent(context, MainActivity::class.java)
    }
}
