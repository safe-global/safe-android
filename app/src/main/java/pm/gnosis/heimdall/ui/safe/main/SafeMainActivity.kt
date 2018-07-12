package pm.gnosis.heimdall.ui.safe.main


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import com.jakewharton.rxbinding2.support.v7.widget.itemClicks
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_content_edit_name.view.*
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
import pm.gnosis.heimdall.ui.debugsettings.DebugSettingsActivity
import pm.gnosis.heimdall.ui.safe.create.CreateSafeIntroActivity
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsFragment
import pm.gnosis.heimdall.ui.safe.list.SafeAdapter
import pm.gnosis.heimdall.ui.safe.pending.DeploySafeProgressFragment
import pm.gnosis.heimdall.ui.safe.pending.PendingSafeFragment
import pm.gnosis.heimdall.ui.settings.general.GeneralSettingsActivity
import pm.gnosis.heimdall.ui.tokens.manage.ManageTokensActivity
import pm.gnosis.heimdall.utils.CustomAlertDialogBuilder
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.model.Solidity
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
        if (screenActive) {
            setupSelectedSafe()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout_safe_main_toolbar_nav_icon.setOnClickListener {
            layout_safe_main_drawer_layout.openDrawer(Gravity.START)
        }

        layout_safe_main_safes_list.itemAnimator = null
        layout_safe_main_safes_list.layoutManager = layoutManager
        layout_safe_main_safes_list.adapter = adapter

        layout_safe_main_debug_settings.visible(BuildConfig.DEBUG)
        layout_safe_main_account.visible(BuildConfig.DEBUG)

        popupMenu = PopupMenu(this, layout_safe_main_toolbar_overflow).apply {
            inflate(R.menu.safe_details_menu)
            menu.findItem(R.id.address_book_entry_details_menu_delete).title = SpannableStringBuilder().appendText(
                getString(R.string.remove_from_device), ForegroundColorSpan(getColorCompat(R.color.tomato))
            )
        }
    }

    override fun onStart() {
        super.onStart()
        screenActive = true
        setupSelectedSafe()

        disposables += layout_safe_main_toolbar_overflow.clicks()
            .subscribeBy { popupMenu.show() }

        updateToolbar()
        setupNavigation()
    }

    private fun setupSelectedSafe() {
        val selectedSafe = intent?.getStringExtra(EXTRA_SELECTED_SAFE)?.hexAsBigIntegerOrNull()
        intent.removeExtra(EXTRA_SELECTED_SAFE)
        disposables += (selectedSafe?.let { viewModel.selectSafe(it) } ?: viewModel.loadSelectedSafe())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = ::showSafe, onError = ::showSafeError)
    }

    private fun setupNavigation() {
        layout_safe_main_selected_safe_background.setOnClickListener {
            toggleSafes(layout_safe_main_navigation_safe_list.visibility == View.GONE)
        }

        layout_safe_main_address_book.setOnClickListener {
            startActivity(AddressBookActivity.createIntent(this))
            closeDrawer()
        }

        layout_safe_main_account.setOnClickListener {
            startActivity(AccountActivity.createIntent(this))
            closeDrawer()
        }

        layout_safe_main_tokens.setOnClickListener {
            startActivity(ManageTokensActivity.createIntent(this))
            closeDrawer()
        }

        layout_safe_main_general_settings.setOnClickListener {
            startActivity(GeneralSettingsActivity.createIntent(this))
            closeDrawer()
        }

        layout_safe_main_add_safe.setOnClickListener {
            startActivity(CreateSafeIntroActivity.createIntent(this))
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
    }

    private fun closeDrawer() {
        layout_safe_main_drawer_layout.closeDrawers()
        toggleSafes(false)
    }

    private fun toggleSafes(visible: Boolean) {
        // If we don't have a safe we should always show the creation button
        layout_safe_main_navigation_safe_creation.visible(visible || selectedSafe == null)
        layout_safe_main_navigation_safe_list.visible(visible)
        layout_safe_main_navigation_settings.visible(!visible)
        layout_safe_main_selected_safe_button.setImageResource(if (visible) R.drawable.ic_close_safe_selection else R.drawable.ic_open_safe_selection)
    }

    private fun toggleHasSafes(hasSafes: Boolean) {
        layout_safe_main_selected_safe_group.visible(hasSafes)
        layout_safe_main_no_safe_message.visible(!hasSafes)
        layout_safe_main_navigation_safe_creation.visible(!hasSafes)
        layout_safe_main_navigation_safe_creation_divider.visible(!hasSafes)
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
        toggleSafes(false)
        toggleHasSafes(false)
    }

    private fun showSafe(safe: AbstractSafe) {
        val hasSafeChanged = selectedSafe != safe
        selectedSafe = safe
        toggleHasSafes(true)
        closeDrawer()
        if (!hasSafeChanged) {
            selectTab()
            return
        }
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
                layout_safe_main_selected_safe_progress.visible(false)
                layout_safe_main_selected_safe_icon.visible(true)
                layout_safe_main_selected_safe_icon.setAddress(safe.address)
                layout_safe_main_toolbar_overflow.visible(true)

                setupOverflowMenu(safe.address, safe.name, safe)
                updateSafeInfo(safe.displayName(this) to safe.address.asEthereumAddressString())
                observeSafeInfo(safe)
            }
            is PendingSafe -> {
                layout_safe_main_selected_safe_icon.visible(false)
                layout_safe_main_selected_safe_progress.visible(true)
                layout_safe_main_toolbar_overflow.visible(true)

                setupOverflowMenu(safe.address, safe.name, safe)
                updateSafeInfo(safe.displayName(this) to safe.address.asEthereumAddressString())
                observeSafeInfo(safe)
            }
            else -> {
                layout_safe_main_selected_safe_name.setText(R.string.no_safe_selected)
                layout_safe_main_selected_safe_info.text = null
                layout_safe_main_selected_safe_icon.visible(false)
                layout_safe_main_selected_safe_progress.visible(false)
                layout_safe_main_toolbar_title.text = getString(R.string.your_safe)
                layout_safe_main_toolbar_overflow.visible(false)
            }
        }
    }

    private fun observeSafeInfo(safe: AbstractSafe) {
        disposables += viewModel.observeSafe(safe)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::updateSafeInfo)
    }

    private fun updateSafeInfo(info: Pair<String, String>) {
        val (name, address) = info
        layout_safe_main_selected_safe_info.text = address
        layout_safe_main_selected_safe_name.text = name
        layout_safe_main_toolbar_title.text = name
    }

    private fun setupOverflowMenu(address: Solidity.Address, name: String?, originalSafe: AbstractSafe) {
        val extendedMenu = originalSafe is Safe
        popupMenu.menu.findItem(R.id.safe_details_menu_sync).isVisible = extendedMenu
        disposables += popupMenu.itemClicks()
            .subscribeBy(onNext = {
                when (it.itemId) {
                    R.id.address_book_entry_details_menu_delete ->
                        removeSafe(originalSafe)
                    R.id.safe_details_menu_rename ->
                        renameSafe(originalSafe, name)
                    R.id.safe_details_menu_sync -> {
                        disposables += viewModel.syncWithChromeExtension(address)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeBy(onComplete = { toast(R.string.sync_successful) },
                                onError = { toast(R.string.error_syncing) })
                    }
                    R.id.safe_details_menu_show_on_etherscan -> {
                        openUrl(getString(R.string.etherscan_address_url, address.asEthereumAddressString()))
                    }
                }
            }, onError = Timber::e)
    }

    private fun renameSafe(safe: AbstractSafe, default: String?) {
        val alertContent = layoutInflater.inflate(R.layout.dialog_content_edit_name, null)
            .apply {
                dialog_content_edit_name_input.apply {
                    setText(default)
                    setSelection(default?.length ?: 0)
                }
            }
        CustomAlertDialogBuilder.build(
            this, getString(R.string.edit_safe_name), alertContent, R.string.save, { dialog ->
                disposables += viewModel.updateSafeName(safe, alertContent.dialog_content_edit_name_input.text.toString())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        snackbar(layout_safe_main_toolbar_title, R.string.update_name_success)
                    }, {
                        errorSnackbar(layout_safe_main_toolbar_title, it)
                    })
                dialog.dismiss()
            }
        )
            .show()
    }

    private fun removeSafe(safe: AbstractSafe) {
        val alertContent = layoutInflater.inflate(R.layout.dialog_content_remove_safe, null)
        CustomAlertDialogBuilder.build(
            this, getString(R.string.remove_safe_title, safe.displayName(this)), alertContent, R.string.remove, { dialog ->
                disposables += viewModel.removeSafe(safe)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        startActivity(SafeMainActivity.createIntent(this))
                        snackbar(layout_safe_main_toolbar_title, getString(R.string.safe_remove_success, safe.displayName(this)))
                    }, {
                        errorSnackbar(layout_safe_main_toolbar_title, it)
                    })
                dialog.dismiss()
            },
            confirmColor = R.color.tomato
        )
            .show()
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
