package pm.gnosis.heimdall.ui.safe.main


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.view.Gravity
import android.view.View
import com.jakewharton.rxbinding2.support.v7.widget.itemClicks
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_safe_main.*
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.R
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
import pm.gnosis.heimdall.ui.safe.create.CreateSafeIntroActivity
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsFragment
import pm.gnosis.heimdall.ui.safe.details.info.SafeSettingsActivity
import pm.gnosis.heimdall.ui.safe.overview.SafeAdapter
import pm.gnosis.heimdall.ui.safe.pending.DeploySafeProgressFragment
import pm.gnosis.heimdall.ui.safe.pending.PendingSafeFragment
import pm.gnosis.heimdall.ui.settings.network.NetworkSettingsActivity
import pm.gnosis.heimdall.ui.settings.security.SecuritySettingsActivity
import pm.gnosis.heimdall.ui.settings.tokens.TokenManagementActivity
import pm.gnosis.svalinn.common.utils.*
import pm.gnosis.utils.asEthereumAddressString
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

    private lateinit var popupMenu: PopupMenu

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
        layout_safe_main_toolbar_nav_icon.setOnClickListener {
            layout_safe_main_drawer_layout.openDrawer(Gravity.START)
        }

        layout_safe_main_safes_list.layoutManager = layoutManager
        layout_safe_main_safes_list.adapter = adapter

        layout_safe_main_debug_settings.visible(BuildConfig.DEBUG)

        popupMenu = PopupMenu(this, layout_safe_main_toolbar_overflow).apply {
            inflate(R.menu.safe_details_menu)
        }
    }

    override fun onStart() {
        super.onStart()
        screenActive = true
        val selectedSafe = intent?.getStringExtra(EXTRA_SELECTED_SAFE)?.hexAsBigIntegerOrNull()
        intent.removeExtra(EXTRA_SELECTED_SAFE)
        disposables += (selectedSafe?.let { viewModel.selectSafe(it) } ?: viewModel.loadSelectedSafe())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = ::showSafe, onError = ::showSafeError)

        disposables += layout_safe_main_toolbar_overflow.clicks()
            .subscribeBy { popupMenu.show() }

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
            startActivity(CreateSafeIntroActivity.createIntent(this))
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
                is PendingSafe -> {
                    replace(
                        R.id.layout_safe_main_content_frame,
                        if (safe.isFunded) DeploySafeProgressFragment.createInstance(safe) else PendingSafeFragment.createInstance(safe)
                    )
                }
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
        popupMenu.setOnMenuItemClickListener(null)
        val safe = selectedSafe
        when (safe) {
            is Safe -> {
                layout_safe_main_selected_safe_icon.visible(true)
                layout_safe_main_selected_safe_icon.setAddress(safe.address)
                val safeName = safe.name ?: getString(R.string.your_safe)
                layout_safe_main_selected_safe_name.text = safeName
                layout_safe_main_toolbar_title.text = safeName
                layout_safe_main_toolbar_overflow.visible(true)

                disposables += popupMenu.itemClicks()
                    .subscribeBy(onNext = {
                        when (it.itemId) {
                            R.id.safe_details_menu_share ->
                                ShareSafeAddressDialog.create(safe.address).show(supportFragmentManager, null)
                            R.id.safe_details_menu_settings ->
                                startActivity(SafeSettingsActivity.createIntent(this, safe.address.asEthereumAddressString()))
                            R.id.safe_details_menu_sync -> {
                                disposables += viewModel.syncWithChromeExtension(safe.address)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribeBy(onComplete = { toast(R.string.sync_successful) },
                                        onError = { toast(R.string.error_syncing) })
                            }

                        }
                    }, onError = Timber::e)
            }
            is PendingSafe -> {
                val safeName = safe.name ?: getString(R.string.your_safe)
                layout_safe_main_selected_safe_name.text = safeName
                layout_safe_main_selected_safe_icon.visible(false)
                layout_safe_main_toolbar_title.text = safeName
                layout_safe_main_toolbar_overflow.visible(false)
            }
            else -> {
                layout_safe_main_selected_safe_name.setText(R.string.no_safe_selected)
                layout_safe_main_selected_safe_icon.visible(false)
                layout_safe_main_toolbar_title.text = getString(R.string.your_safe)
                layout_safe_main_toolbar_overflow.visible(false)
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
