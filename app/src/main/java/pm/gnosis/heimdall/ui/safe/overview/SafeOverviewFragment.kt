package pm.gnosis.heimdall.ui.safe.overview

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_safe_add_input.view.*
import kotlinx.android.synthetic.main.layout_safe_overview.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.ZxingIntentIntegrator.REQUEST_CODE
import pm.gnosis.heimdall.common.utils.ZxingIntentIntegrator.SCAN_RESULT_EXTRA
import pm.gnosis.heimdall.common.utils.scanQrCode
import pm.gnosis.heimdall.common.utils.snackbar
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.common.utils.toast
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsActivity
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.isValidEthereumAddress
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

class SafeOverviewFragment : BaseFragment() {
    @Inject
    lateinit var viewModel: SafeOverviewViewModel
    @Inject
    lateinit var adapter: SafeAdapter
    @Inject
    lateinit var layoutManager: LinearLayoutManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.layout_safe_overview, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        layout_safe_overview_list.layoutManager = layoutManager
        layout_safe_overview_list.adapter = adapter
    }

    override fun onDestroyView() {
        layout_safe_overview_list.layoutManager = null
        super.onDestroyView()
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
        startActivity(SafeDetailsActivity.createIntent(context!!, safe))
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
                .applicationComponent(component)
                .viewModule(ViewModule(context!!))
                .build().inject(this)
    }
}
