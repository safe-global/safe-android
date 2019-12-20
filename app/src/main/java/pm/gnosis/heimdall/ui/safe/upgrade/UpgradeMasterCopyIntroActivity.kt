package pm.gnosis.heimdall.ui.safe.upgrade

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_upgrade_master_copy_intro.*
import kotlinx.android.synthetic.main.layout_upgrade_master_copy_intro_page_1.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.utils.setupLink
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString


class UpgradeMasterCopyIntroActivity : BaseActivity() {

    private val layouts =
        listOf(
            R.layout.layout_upgrade_master_copy_intro_page_1,
            R.layout.layout_upgrade_master_copy_intro_page_2,
            R.layout.layout_upgrade_master_copy_intro_page_3
        )

    override fun screenId(): ScreenId? = ScreenId.UPGRADE_MASTER_COPY_INTRO

    private val viewPager = object : PagerAdapter() {

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val inflater = LayoutInflater.from(container.context)
            val layoutId = layouts[position]
            val layout = inflater.inflate(layoutId, container, false) as ViewGroup
            if (layoutId == R.layout.layout_upgrade_master_copy_intro_page_1) {
                layout.upgrade_master_copy_intro_page_1_blog_link
                    .setupLink(getString(R.string.blog_link), getString(R.string.more_info_in_our_blog))
            }
            container.addView(layout)
            return layout
        }

        override fun destroyItem(container: ViewGroup, position: Int, any: Any) {
            (any as? View)?.let { container.removeView(it) }
        }

        override fun isViewFromObject(view: View, any: Any) = view == any

        override fun getCount(): Int = layouts.size
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_upgrade_master_copy_intro)
        upgrade_master_copy_intro_pager.adapter = viewPager
        upgrade_master_copy_intro_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                updateUi(position)
            }
        })
        upgrade_master_copy_intro_indicator.setViewPager(upgrade_master_copy_intro_pager)
        upgrade_master_copy_intro_button.setOnClickListener {
            if (upgrade_master_copy_intro_pager.currentItem == (layouts.size - 1)) {
                val safeAddress = intent.getStringExtra(EXTRA_SAFE_ADDRESS)?.asEthereumAddress()!!
                startActivity(UpgradeMasterCopyActivity.createIntent(this, safeAddress))
            } else
                upgrade_master_copy_intro_pager.currentItem = upgrade_master_copy_intro_pager.currentItem + 1
        }
        upgrade_master_copy_intro_back_arrow.setOnClickListener { onBackPressed() }
    }

    override fun onStart() {
        super.onStart()
        updateUi(upgrade_master_copy_intro_pager.currentItem)

        disposables += upgrade_master_copy_intro_back_arrow.clicks()
            .subscribeBy { onBackPressed() }
    }

    private fun updateUi(position: Int) {
        if ((layouts.size - 1) == position) {
            upgrade_master_copy_intro_button.text = getString(R.string.get_started)
        } else {
            upgrade_master_copy_intro_button.text = getString(R.string.next)
        }
    }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"
        fun createIntent(context: Context, safe: Solidity.Address) = Intent(context, UpgradeMasterCopyIntroActivity::class.java).apply {
            putExtra(EXTRA_SAFE_ADDRESS, safe.asEthereumAddressString())
        }
    }

}
