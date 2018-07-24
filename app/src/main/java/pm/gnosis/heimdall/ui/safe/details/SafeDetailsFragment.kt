package pm.gnosis.heimdall.ui.safe.details

import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_safe_details.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.reporting.TabId
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.base.FactoryPagerAdapter
import pm.gnosis.heimdall.ui.base.ScrollableContainer
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsFragment
import pm.gnosis.heimdall.ui.tokens.balances.TokenBalancesFragment
import pm.gnosis.heimdall.ui.tokens.receive.ReceiveTokenActivity
import pm.gnosis.heimdall.ui.tokens.select.SelectTokenActivity
import pm.gnosis.heimdall.utils.setCompoundDrawableResource
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.withArgs
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Inject

class SafeDetailsFragment : BaseFragment() {

    @Inject
    lateinit var viewModel: SafeDetailsContract

    @Inject
    lateinit var eventTracker: EventTracker

    private val items = listOf(R.string.tab_title_assets, R.string.tab_title_transactions)

    private lateinit var pagerAdapter: FactoryPagerAdapter

    private lateinit var safeAddress: Solidity.Address
    private var safeName: String? = null
    private var tabToSelect: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabToSelect = arguments?.getInt(EXTRA_SELECTED_TAB, tabToSelect) ?: tabToSelect
        arguments?.remove(EXTRA_SELECTED_TAB)
        pagerAdapter = pagerAdapter()
        layout_safe_details_send_button.setCompoundDrawableResource(left = R.drawable.ic_send_azure)
        layout_safe_details_receive_button.setCompoundDrawableResource(left = R.drawable.ic_qrcode_scan_azure)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.layout_safe_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        safeAddress = arguments?.getString(EXTRA_SAFE_ADDRESS)?.asEthereumAddress()!!
        safeName = arguments?.getString(EXTRA_SAFE_NAME)
        viewModel.setup(safeAddress, safeName)

        layout_safe_details_viewpager.adapter = pagerAdapter
        layout_safe_details_viewpager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                layout_safe_details_appbar.setExpanded(true, true)
                positionToTabID(position)?.let { eventTracker.submit(Event.TabSelect(it)) }
            }
        })
        setupTabLayout()
        setSelectedTab()
    }

    private fun setupTabLayout() {
        layout_safe_details_tabbar.setupWithViewPager(layout_safe_details_viewpager)
        (0 until layout_safe_details_tabbar.tabCount).forEach {
            // We need to set this manually as 'setupWithViewPager' resets the layout specified in the xml
            layout_safe_details_tabbar.getTabAt(it)?.apply {
                setCustomView(R.layout.layout_tab_item)
                positionToIcon(it)?.let {
                    setIcon(it)
                } ?: run {
                    setIcon(null)
                }
            }
        }
    }

    private fun setSelectedTab() {
        val tabPosition = items.indexOf(tabToSelect)
        if (tabPosition >= 0) {
            layout_safe_details_viewpager.currentItem = tabPosition
            (pagerAdapter.getItem(tabPosition) as? ScrollableContainer)?.scrollToTop()
        }
        tabToSelect = 0
    }

    fun selectTab(@StringRes tabId: Int) {
        tabToSelect = tabId
        view ?: return
        setSelectedTab()
    }

    override fun onStart() {
        super.onStart()
        disposables += viewModel.observeSafe()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = {
                safeName = it.name
            }, onError = Timber::e)

        disposables += layout_safe_details_send_button.clicks()
            .subscribeBy { startActivity(SelectTokenActivity.createIntent(context!!, safeAddress)) }

        disposables += layout_safe_details_receive_button.clicks()
            .subscribeBy { startActivity(ReceiveTokenActivity.createIntent(context!!, safeAddress)) }
    }

    private fun positionToId(position: Int) = items.getOrElse(position) { -1 }

    private fun positionToTabID(position: Int) =
        when (positionToId(position)) {
            R.string.tab_title_assets -> {
                TabId.SAFE_DETAILS_ASSETS
            }
            R.string.tab_title_transactions -> {
                TabId.SAFE_DETAILS_TRANSACTIONS
            }
            else -> null
        }

    private fun positionToIcon(position: Int) =
        when (positionToId(position)) {
            R.string.tab_title_assets -> {
                R.drawable.ic_tokens
            }
            R.string.tab_title_transactions -> {
                R.drawable.ic_transaction_white_24dp
            }
            else -> null
        }

    private fun pagerAdapter() = FactoryPagerAdapter(childFragmentManager, FactoryPagerAdapter.Factory(items.size, {
        when (positionToId(it)) {
            R.string.tab_title_assets -> {
                TokenBalancesFragment.createInstance(safeAddress, trackingEnabled = true)
            }
            R.string.tab_title_transactions -> {
                SafeTransactionsFragment.createInstance(safeAddress)
            }
            else -> throw IllegalStateException("Unhandled tab position")
        }
    }, {
        getString(items[it])
    }))

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
            .applicationComponent(component)
            .viewModule(ViewModule(context!!))
            .build()
            .inject(this)
    }

    companion object {
        private const val EXTRA_SAFE_NAME = "extra.string.safe_name"
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"
        private const val EXTRA_SELECTED_TAB = "extra.int.selected_tab"

        fun createInstance(safe: Safe, @StringRes selectedTab: Int = 0) =
            SafeDetailsFragment().withArgs(
                Bundle().apply {
                    putString(EXTRA_SAFE_NAME, safe.name)
                    putString(EXTRA_SAFE_ADDRESS, safe.address.asEthereumAddressString())
                    putInt(EXTRA_SELECTED_TAB, selectedTab)
                }
            )
    }
}
