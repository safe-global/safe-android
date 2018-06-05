package pm.gnosis.heimdall.ui.safe.main


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.Gravity
import android.view.View
import com.jakewharton.rxbinding2.support.v7.widget.itemClicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_safe_main.*
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.account.AccountActivity
import pm.gnosis.heimdall.ui.addressbook.list.AddressBookActivity
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.credits.BuyCreditsActivity
import pm.gnosis.heimdall.ui.debugsettings.DebugSettingsActivity
import pm.gnosis.heimdall.ui.dialogs.share.ShareSafeAddressDialog
import pm.gnosis.heimdall.ui.safe.add.AddSafeActivity
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsFragment
import pm.gnosis.heimdall.ui.safe.details.info.SafeSettingsActivity
import pm.gnosis.heimdall.ui.safe.overview.SafeAdapter
import pm.gnosis.heimdall.ui.settings.network.NetworkSettingsActivity
import pm.gnosis.heimdall.ui.settings.security.SecuritySettingsActivity
import pm.gnosis.heimdall.ui.settings.tokens.TokenManagementActivity
import pm.gnosis.heimdall.ui.transactions.view.review.ReviewTransactionActivity
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.transaction
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.hexAsBigIntegerOrNull
import pm.gnosis.utils.toHexString
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

class SafeMainActivity : ViewModelActivity<SafeMainContract>() {

    @Inject
    lateinit var adapter: SafeAdapter

    @Inject
    lateinit var layoutManager: LinearLayoutManager

    private var selectedSafe: AbstractSafe? = null

    private var screenActive: Boolean = false

    override fun screenId() = ScreenId.SAFE_MAIN

    override fun layout() = R.layout.layout_safe_main

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.intent = intent
        val selectedSafe = intent?.getStringExtra(EXTRA_SELECTED_SAFE)?.hexAsBigIntegerOrNull()
        if (selectedSafe != null && screenActive) {
            intent.removeExtra(EXTRA_SELECTED_SAFE)
            disposables += viewModel.selectSafe(selectedSafe)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onSuccess = ::showSafe, onError = Timber::e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout_safe_main_toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp)
        layout_safe_main_toolbar.setNavigationOnClickListener {
            layout_safe_main_drawer_layout.openDrawer(Gravity.START)
        }

        layout_safe_main_safes_list.layoutManager = layoutManager
        layout_safe_main_safes_list.adapter = adapter

        layout_safe_main_debug_settings.visible(BuildConfig.DEBUG)
    }

    override fun onStart() {
        super.onStart()
        screenActive = true
        val selectedSafe = intent?.getStringExtra(EXTRA_SELECTED_SAFE)?.hexAsBigIntegerOrNull()
        intent.removeExtra(EXTRA_SELECTED_SAFE)
        disposables += (selectedSafe?.let { viewModel.selectSafe(it) } ?: viewModel.loadSelectedSafe())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = ::showSafe, onError = ::showSafeError)
        updateToolbar()
        setupNavigation()
    }

    private fun setupNavigation() {
        layout_safe_main_navigation_header_background.setOnClickListener {
            toggleSafes(layout_safe_main_navigation_safes.visibility == View.GONE)
        }
        layout_safe_main_address_book.setOnClickListener {
            startActivity(AddressBookActivity.createIntent(this))
            closeDrawer()
        }

        layout_safe_main_account.setOnClickListener {
            startActivity(AccountActivity.createIntent(this))
            closeDrawer()
        }

        layout_safe_main_network.setOnClickListener {
            startActivity(NetworkSettingsActivity.createIntent(this))
            closeDrawer()
        }

        layout_safe_main_tokens.setOnClickListener {
            startActivity(TokenManagementActivity.createIntent(this))
            closeDrawer()
        }

        layout_safe_main_security.setOnClickListener {
            startActivity(SecuritySettingsActivity.createIntent(this))
            closeDrawer()
        }

        layout_safe_main_add_safe.setOnClickListener {
            startActivity(AddSafeActivity.createIntent(this))
            closeDrawer()
        }

        layout_safe_main_credits.setOnClickListener {
            startActivity(BuyCreditsActivity.createIntent(this))
            closeDrawer()
        }

        layout_safe_main_debug_settings.setOnClickListener {
            startActivity(DebugSettingsActivity.createIntent(this))
            closeDrawer()
        }

        disposables += viewModel.observeSafes()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onSafes, onError = ::onSafesError)

        disposables += adapter.safeSelection
            .flatMapSingle {
                viewModel.selectSafe(
                    when (it) {
                        is Safe -> it.address.value
                        is PendingSafe -> it.hash
                    }
                ).mapToResult()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::showSafe, onError = Timber::e)

        disposables += adapter.shareSelection
            .subscribeBy(onNext = {
                ShareSafeAddressDialog.create(it).show(supportFragmentManager, null)
            }, onError = Timber::e)
    }

    private fun closeDrawer() {
        layout_safe_main_drawer_layout.closeDrawers()
        toggleSafes(false)
    }

    private fun toggleSafes(visible: Boolean) {
        layout_safe_main_navigation_safes.visible(visible)
        layout_safe_main_navigation_settings.visible(!visible)
        val arrowDrawable = if (visible) R.drawable.ic_arrow_drop_up_white_24dp else R.drawable.ic_arrow_drop_down_white_24dp
        layout_safe_main_selected_safe_name.setCompoundDrawablesWithIntrinsicBounds(0, 0, arrowDrawable, 0)
    }

    private fun onSafes(data: Adapter.Data<AbstractSafe>) {
        adapter.updateData(data)
    }

    private fun onSafesError(throwable: Throwable) {
        Timber.e(throwable)
    }

    override fun onStop() {
        screenActive = false
        super.onStop()
    }

    private fun showSafeError(throwable: Throwable) {
        Timber.e(throwable)
        selectedSafe = null
        supportFragmentManager.transaction {
            replace(R.id.layout_safe_main_content_frame, NoSafesFragment())
        }
        updateToolbar()
    }

    private fun showSafe(safe: AbstractSafe) {
        closeDrawer()
        if (selectedSafe == safe) {
            selectTab()
            return
        }
        selectedSafe = safe
        supportFragmentManager.transaction {
            when (safe) {
                is Safe -> {
                    val selectedTab = intent.getIntExtra(EXTRA_SELECTED_TAB, 0)
                    intent.removeExtra(EXTRA_SELECTED_TAB)
                    replace(R.id.layout_safe_main_content_frame, SafeDetailsFragment.createInstance(safe, selectedTab))
                }
                is PendingSafe -> replace(R.id.layout_safe_main_content_frame, PendingSafeFragment.createInstance(safe))
            }
        }
        updateToolbar()
    }

    private fun selectTab() {
        val selectedTab = intent.getIntExtra(EXTRA_SELECTED_TAB, 0)
        intent.removeExtra(EXTRA_SELECTED_TAB)
        if (selectedTab == 0) return
        (supportFragmentManager.findFragmentById(R.id.layout_safe_main_content_frame) as? SafeDetailsFragment)?.selectTab(selectedTab)
    }

    private fun updateToolbar() {
        val safe = selectedSafe
        layout_safe_main_toolbar.menu.clear()
        when (safe) {
            is Safe -> {
                layout_safe_main_selected_safe_icon.visible(true)
                layout_safe_main_selected_safe_icon.setAddress(safe.address)
                layout_safe_main_selected_safe_name.text = safe.name
                layout_safe_main_toolbar.title = safe.name
                layout_safe_main_toolbar.subtitle = safe.address.asEthereumAddressString()

                layout_safe_main_toolbar.inflateMenu(R.menu.safe_details_menu)
                disposables += layout_safe_main_toolbar.itemClicks()
                    .subscribeBy(onNext = {
                        when (it.itemId) {
                            R.id.safe_details_menu_share ->
                                ShareSafeAddressDialog.create(safe.address).show(supportFragmentManager, null)
                            R.id.safe_details_menu_settings ->
                                startActivity(SafeSettingsActivity.createIntent(this, safe.address.asEthereumAddressString()))
                        }
                    }, onError = Timber::e)
            }
            is PendingSafe -> {
                layout_safe_main_selected_safe_name.text = safe.name
                layout_safe_main_selected_safe_icon.visible(false)
                layout_safe_main_toolbar.title = safe.name
                layout_safe_main_toolbar.subtitle = null
            }
            else -> {
                layout_safe_main_selected_safe_name.setText(R.string.no_safe_selected)
                layout_safe_main_selected_safe_icon.visible(false)
                layout_safe_main_toolbar.setTitle(R.string.app_name)
                layout_safe_main_toolbar.subtitle = null
            }
        }
    }

    companion object {
        private const val EXTRA_SELECTED_SAFE = "extra.string.selected_safe"
        private const val EXTRA_SELECTED_TAB = "extra.integer.selected_tab"

        fun createIntent(context: Context, selectedSafeAddressOrHash: BigInteger? = null, selectedTab: Int = 0) =
            Intent(context, SafeMainActivity::class.java).apply {
                putExtra(EXTRA_SELECTED_SAFE, selectedSafeAddressOrHash?.toHexString())
                putExtra(EXTRA_SELECTED_TAB, selectedTab)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
    }
}
