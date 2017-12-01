package pm.gnosis.heimdall.ui.safe.overview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_safe_overview.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.authenticate.AuthenticateActivity
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.safe.add.AddSafeActivity
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsActivity
import pm.gnosis.heimdall.ui.settings.SettingsActivity
import timber.log.Timber
import javax.inject.Inject

class SafesOverviewActivity : BaseActivity() {
    @Inject
    lateinit var viewModel: SafeOverviewViewModel
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
            startActivity(AuthenticateActivity.createIntent(this))
        }
    }

    override fun onStart() {
        super.onStart()
        disposables += viewModel.observeSafes()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(onNext = this::onSafes, onError = this::onSafesError)

        disposables += adapter.safeSelection
                .subscribeBy(onNext = this::onSafeSelection, onError = Timber::e)
    }

    private fun onSafes(data: Adapter.Data<AbstractSafe>) {
        adapter.updateData(data)
        layout_safe_overview_empty_view.visibility = if (data.entries.isEmpty()) View.VISIBLE else View.GONE
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
