package pm.gnosis.heimdall.ui.safe.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_add_existing_safe.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.utils.errorSnackbar
import javax.inject.Inject


class AddExistingSafeFragment : BaseFragment() {

    @Inject
    lateinit var viewModel: AddSafeContract

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater.inflate(R.layout.layout_add_existing_safe, container, false)!!

    override fun onStart() {
        super.onStart()
        disposables += layout_add_existing_safe_add_button.clicks().flatMap {
            viewModel.addExistingSafe(layout_add_existing_safe_name_input.text.toString(), layout_add_existing_safe_address_input.text.toString())
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { toggleAdding(true) }
                    .doAfterTerminate { toggleAdding(false) }
        }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(::safeAdded, ::errorDeploying)
    }

    private fun toggleAdding(inProgress: Boolean) {
        layout_add_existing_safe_add_button.isEnabled = !inProgress
        layout_add_existing_safe_name_input.isEnabled = !inProgress
        layout_add_existing_safe_progress.visibility = if (inProgress) View.VISIBLE else View.GONE
    }

    private fun safeAdded(ignored: Unit) {
        activity?.finish()
    }

    private fun errorDeploying(throwable: Throwable) {
        view?.let { errorSnackbar(it, throwable) }
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
                .applicationComponent(component)
                .viewModule(ViewModule(context!!))
                .build().inject(this)
    }

    companion object {
        fun createInstance() = AddExistingSafeFragment()
    }
}
