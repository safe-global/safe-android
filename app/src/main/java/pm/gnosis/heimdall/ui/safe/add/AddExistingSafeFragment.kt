package pm.gnosis.heimdall.ui.safe.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_add_existing_safe.*
import kotlinx.android.synthetic.main.layout_address_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.isValidEthereumAddress
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AddExistingSafeFragment : BaseFragment() {
    @Inject
    lateinit var viewModel: AddSafeContract

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.layout_add_existing_safe, container, false)!!

    override fun onStart() {
        super.onStart()
        disposables += layout_add_existing_safe_add_button.clicks()
            .flatMapSingle {
                viewModel.addExistingSafe(layout_add_existing_safe_name_input.text.toString(), layout_add_existing_safe_address_input.text.toString())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { toggleAdding(true) }
                    .doAfterTerminate { toggleAdding(false) }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(::safeAdded, ::errorDeploying)

        disposables += Observable
            .combineLatest(
                viewModel.loadActiveAccount(),
                observeSafeAddressTextChanges(),
                BiFunction { account: Account, safeAddress: String -> account to safeAddress })
            .flatMap { (account, safeAddress) ->
                viewModel.loadSafeInfo(safeAddress)
                    .map { it.map { account to it } }
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { layout_add_existing_safe_owners_progress.visibility = View.VISIBLE }
                    .doOnTerminate { layout_add_existing_safe_owners_progress.visibility = View.GONE }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(
                onNext = { (account, safeInfo) -> onSafeInfo(account, safeInfo) },
                onError = ::onSafeInfoError
            )
    }

    private fun observeSafeAddressTextChanges() = layout_add_existing_safe_address_input.textChanges()
        .debounce(500, TimeUnit.MILLISECONDS)
        .map { it.toString() }
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext { layout_add_existing_safe_owners_container.visibility = View.GONE }
        .filter { it.isValidEthereumAddress() }

    private fun onSafeInfo(account: Account, safeInfo: SafeInfo) {
        layout_add_existing_safe_owners.removeAllViews()
        safeInfo.owners.forEach { address ->
            val view = layoutInflater.inflate(R.layout.layout_address_item, layout_add_existing_safe_owners, false)
            if (account.address == address) {
                view.layout_address_item_name.visibility = View.VISIBLE
                view.layout_address_item_name.text = getString(R.string.this_device)
            }
            view.layout_address_item_icon.setAddress(address)
            view.layout_address_item_value.text = address.asEthereumAddressString()
            layout_add_existing_safe_owners.addView(view)
        }
        layout_add_existing_safe_owners_container.visibility = View.VISIBLE

    }

    private fun onSafeInfoError(throwable: Throwable) {
        Timber.e(throwable)
        view?.let { errorSnackbar(it, throwable) }
    }

    private fun toggleAdding(inProgress: Boolean) {
        layout_add_existing_safe_add_button.isEnabled = !inProgress
        layout_add_existing_safe_name_input.isEnabled = !inProgress
        layout_add_existing_safe_progress.visibility = if (inProgress) View.VISIBLE else View.GONE
    }

    private fun safeAdded(address: Solidity.Address) {
        startActivity(SafeMainActivity.createIntent(context!!, address.value))
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
