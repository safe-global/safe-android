package pm.gnosis.heimdall.ui.walletconnect.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Single
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_wallet_connect_intro.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.walletconnect.sessions.WalletConnectSessionsActivity
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString


class WalletConnectIntroActivity : ViewModelActivity<WalletConnectIntroContract>() {

    private val layouts =
        listOf(R.layout.layout_wallet_connect_intro_page_1, R.layout.layout_wallet_connect_intro_page_2, R.layout.layout_wallet_connect_intro_page_3)

    override fun layout(): Int = R.layout.layout_wallet_connect_intro

    override fun inject(component: ViewComponent) = component.inject(this)

    // TODO: tracked manually because of view pager (as soon as tracking is defined)
    override fun screenId(): ScreenId? = ScreenId.WALLET_CONNECT_INTRO

    private val viewPager = object : PagerAdapter() {

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val inflater = LayoutInflater.from(container.context)
            val layout = inflater.inflate(layouts[position], container, false) as ViewGroup
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
        layout_wallet_connect_intro_pager.adapter = viewPager
        layout_wallet_connect_intro_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                updateUi(position)
            }
        })
        layout_wallet_connect_intro_indicator.setViewPager(layout_wallet_connect_intro_pager)
    }

    override fun onStart() {
        super.onStart()
        updateUi(layout_wallet_connect_intro_pager.currentItem)
        disposables += layout_wallet_connect_intro_button.clicks()
            .switchMapSingle {
                val introDone = layout_wallet_connect_intro_pager.currentItem == (layouts.size - 1)
                if (introDone) viewModel.markIntroDone().andThen(Single.just(true))
                else Single.just(false)
            }
            .subscribeBy {
                if (it) {
                    val safeAddress = intent.getStringExtra(EXTRA_SAFE_ADDRESS)?.asEthereumAddress()!!
                    startActivity(WalletConnectSessionsActivity.createIntent(this, safeAddress))
                    finish()
                } else
                    layout_wallet_connect_intro_pager.currentItem = layout_wallet_connect_intro_pager.currentItem + 1

            }
        disposables+= layout_wallet_connect_intro_back_arrow.clicks()
            .subscribeBy { onBackPressed() }
    }

    private fun updateUi(position: Int) {
        if ((layouts.size - 1) == position) {
            layout_wallet_connect_intro_button.text = getString(R.string.get_started)
        } else {
            layout_wallet_connect_intro_button.text = getString(R.string.next)
        }
    }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"
        fun createIntent(context: Context, safe: Solidity.Address) = Intent(context, WalletConnectIntroActivity::class.java).apply {
            putExtra(EXTRA_SAFE_ADDRESS, safe.asEthereumAddressString())
        }
    }

}
