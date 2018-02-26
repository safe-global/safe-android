package pm.gnosis.heimdall.ui.safe.overview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_low_balance.*
import kotlinx.android.synthetic.main.layout_safe_overview.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.common.utils.visible
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.reporting.ButtonId
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.account.AccountActivity
import pm.gnosis.heimdall.ui.authenticate.AuthenticateActivity
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.dialogs.share.ShareSafeAddressDialog
import pm.gnosis.heimdall.ui.safe.add.AddSafeActivity
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsActivity
import pm.gnosis.heimdall.ui.settings.SettingsActivity
import pm.gnosis.utils.stringWithNoTrailingZeroes
import timber.log.Timber
import javax.inject.Inject

class SafesOverviewActivity : BaseActivity() {

    override fun screenId() = ScreenId.SAFE_OVERVIEW

    @Inject
    lateinit var viewModel: SafeOverviewContract
    @Inject
    lateinit var adapter: SafeAdapter
    @Inject
    lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_safe_overview)
        layout_safe_overview_toolbar.inflateMenu(R.menu.safes_overview_menu)
        layout_safe_overview_toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.safes_overview_menu_settings -> startActivity(SettingsActivity.createIntent(this))
                R.id.safes_overview_menu_add_safe -> startActivity(AddSafeActivity.createIntent(this))
            }
            true
        }
        layout_safe_overview_list.layoutManager = layoutManager
        layout_safe_overview_list.adapter = adapter

        layout_safe_overview_fab.setOnClickListener {
            eventTracker.submit(Event.ButtonClick(ButtonId.SAFE_OVERVIEW_SCAN_TRANSACTION))
            startActivity(AuthenticateActivity.createIntent(this))
        }

        layout_low_balance_info.text =
                getString(R.string.low_balance_warning, SafeOverviewContract.LOW_BALANCE_THRESHOLD.toEther().stringWithNoTrailingZeroes())

        layout_low_balance_dismiss.setOnClickListener {
            layout_low_balance_root.hide()
            viewModel.dismissHasLowBalance()
        }

        layout_low_balance_view_account.setOnClickListener {
            startActivity(AccountActivity.createIntent(this))
        }
    }

    override fun onStart() {
        super.onStart()
        disposables += viewModel.observeSafes()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onSafes, onError = ::onSafesError)

        disposables += adapter.safeSelection
            .subscribeBy(onNext = ::onSafeSelection, onError = Timber::e)

        disposables += adapter.shareSelection
            .subscribeBy(onNext = {
                ShareSafeAddressDialog.create(it).show(supportFragmentManager, null)
            }, onError = Timber::e)

        disposables += layout_safe_overview_add_safe.clicks()
            .subscribeBy(
                onNext = { startActivity(AddSafeActivity.createIntent(this)) },
                onError = Timber::e
            )

        disposables += viewModel.shouldShowLowBalanceView()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::shouldShowLowBalanceView, onError = Timber::e)
    }

    private fun shouldShowLowBalanceView(show: Boolean) = layout_low_balance_root.run { if (show) show() else hide() }

    private fun onSafes(data: Adapter.Data<AbstractSafe>) {
        adapter.updateData(data)
        layout_safe_overview_empty_view.visible(data.entries.isEmpty())
    }

    private fun onSafesError(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun onSafeSelection(safe: Safe) {
        startActivity(SafeDetailsActivity.createIntent(this, safe))
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this].component)
            .viewModule(ViewModule(this))
            .build().inject(this)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, SafesOverviewActivity::class.java)
    }
}
