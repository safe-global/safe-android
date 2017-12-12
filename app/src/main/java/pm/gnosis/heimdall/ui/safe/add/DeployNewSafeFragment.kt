package pm.gnosis.heimdall.ui.safe.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_deploy_new_safe.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.models.Wei
import pm.gnosis.utils.asDecimalString
import timber.log.Timber
import javax.inject.Inject


class DeployNewSafeFragment : BaseFragment() {

    @Inject
    lateinit var viewModel: AddSafeContract

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater.inflate(R.layout.layout_deploy_new_safe, container, false)!!

    override fun onStart() {
        super.onStart()
        disposables += layout_deploy_new_safe_deploy_button.clicks().flatMap {
            viewModel.deployNewSafe(layout_deploy_new_safe_name_input.text.toString())
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { toggleDeploying(true) }
                    .doAfterTerminate { toggleDeploying(false) }
        }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(::safeDeployed, ::errorDeploying)
        disposables += viewModel.observeEstimate()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::updateEstimate, Timber::e)
    }

    private fun toggleDeploying(inProgress: Boolean) {
        layout_deploy_new_safe_deploy_button.isEnabled = !inProgress
        layout_deploy_new_safe_name_input.isEnabled = !inProgress
        layout_deploy_new_safe_progress.visibility = if (inProgress) View.VISIBLE else View.GONE
    }

    private fun safeDeployed(ignored: Unit) {
        activity?.finish()
    }

    private fun errorDeploying(throwable: Throwable) {
        view?.let { errorSnackbar(it, throwable) }
    }

    private fun updateEstimate(estimate: Wei) {
        layout_deploy_new_safe_transaction_fee.text = getString(R.string.x_wei, estimate.value.asDecimalString())
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
                .applicationComponent(component)
                .viewModule(ViewModule(context!!))
                .build().inject(this)
    }

    companion object {
        fun createInstance() = DeployNewSafeFragment()
    }
}