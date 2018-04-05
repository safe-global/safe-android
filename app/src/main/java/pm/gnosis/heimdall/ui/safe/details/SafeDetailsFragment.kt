package pm.gnosis.heimdall.ui.safe.details

import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_safe_details.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.reporting.ButtonId
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.reporting.TabId
import pm.gnosis.heimdall.ui.authenticate.AuthenticateActivity
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.base.FactoryPagerAdapter
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsFragment
import pm.gnosis.heimdall.ui.tokens.balances.TokenBalancesFragment
import pm.gnosis.svalinn.common.utils.withArgs
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsEthereumAddress
import timber.log.Timber
import javax.inject.Inject

class SafeDetailsFragment : BaseFragment() {

    @Inject
    lateinit var viewModel: SafeDetailsContract

    @Inject
    lateinit var eventTracker: EventTracker

    private val items = listOf(R.string.tab_title_assets, R.string.tab_title_transactions)

    private lateinit var safeAddress: String
    private var safeName: String? = null
    private var tabToSelect: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabToSelect = arguments?.getInt(EXTRA_SELECTED_TAB, tabToSelect) ?: tabToSelect
        arguments?.remove(EXTRA_SELECTED_TAB)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.layout_safe_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        safeAddress = arguments?.getString(EXTRA_SAFE_ADDRESS)!!
        safeName = arguments?.getString(EXTRA_SAFE_NAME)
        viewModel.setup(safeAddress.hexAsEthereumAddress(), safeName)

        layout_safe_details_fab.setOnClickListener {
            eventTracker.submit(Event.ButtonClick(ButtonId.SAFE_DETAILS_CREATE_TRANSACTION))
            startActivity(AuthenticateActivity.createIntent(context!!))
        }
        layout_safe_details_viewpager.adapter = pagerAdapter()
        layout_safe_details_tabbar.setupWithViewPager(layout_safe_details_viewpager)
        layout_safe_details_viewpager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                layout_safe_details_appbar.setExpanded(true, true)
                positionToTabID(position)?.let { eventTracker.submit(Event.TabSelect(it)) }
            }
        })
        setSelectedTab()
    }

    private fun setSelectedTab() {
        layout_safe_details_viewpager.currentItem = items.indexOf(tabToSelect)
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
    }

    private fun positionToId(position: Int) = items.getOrElse(position, { -1 })

    private fun positionToTabID(position: Int) = when (positionToId(position)) {
        R.string.tab_title_assets -> {
            TabId.SAFE_DETAILS_ASSETS
        }
        R.string.tab_title_transactions -> {
            TabId.SAFE_DETAILS_TRANSACTIONS
        }
        else -> null
    }

    private fun pagerAdapter() = FactoryPagerAdapter(childFragmentManager, FactoryPagerAdapter.Factory(items.size, {
        when (positionToId(it)) {
            R.string.tab_title_assets -> {
                TokenBalancesFragment.createInstance(safeAddress.asEthereumAddressString())
            }
            R.string.tab_title_transactions -> {
                SafeTransactionsFragment.createInstance(safeAddress.asEthereumAddressString())
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
