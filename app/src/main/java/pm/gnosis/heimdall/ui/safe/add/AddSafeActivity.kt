package pm.gnosis.heimdall.ui.safe.add

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.view.ViewPager
import kotlinx.android.synthetic.main.layout_add_safe.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.base.FactoryPagerAdapter


class AddSafeActivity : BaseActivity() {

    private val items = listOf(R.string.tab_title_create_new, R.string.tab_title_add_existing)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_add_safe)

        registerToolbar(layout_add_safe_toolbar)
        layout_add_safe_toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        layout_add_safe_toolbar.setTitle(R.string.add_safe)

        layout_add_safe_viewpager.adapter = pagerAdapter()
        layout_add_safe_tabbar.setupWithViewPager(layout_add_safe_viewpager)
        layout_add_safe_viewpager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                layout_add_safe_appbar.setExpanded(true, true)
            }
        })
    }

    private fun positionToId(position: Int) = items.getOrElse(position, { -1 })

    private fun pagerAdapter() = FactoryPagerAdapter(supportFragmentManager, FactoryPagerAdapter.Factory(items.size, {
        when (positionToId(it)) {
            R.string.tab_title_add_existing -> {
                AddExistingSafeFragment.createInstance()
            }
            R.string.tab_title_create_new -> {
                DeployNewSafeFragment.createInstance()
            }
            else -> throw IllegalStateException("Unhandled tab position")
        }
    }, {
        getString(items[it])
    }))

    companion object {
        fun createIntent(context: Context) = Intent(context, AddSafeActivity::class.java)
    }
}