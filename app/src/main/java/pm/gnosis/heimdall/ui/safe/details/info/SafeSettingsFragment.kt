package pm.gnosis.heimdall.ui.safe.details.info

import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.support.v4.widget.refreshes
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_address_item.view.*
import kotlinx.android.synthetic.main.layout_safe_settings.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.*
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.dialogs.share.SimpleAddressShareDialog
import pm.gnosis.heimdall.ui.safe.overview.SafesOverviewActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.utils.hexAsBigInteger
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class SafeSettingsFragment : BaseFragment() {
    @Inject
    lateinit var viewModel: SafeSettingsContract

    private val removeSafeClicks = PublishSubject.create<Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setup(arguments!!.getString(ARGUMENT_SAFE_ADDRESS).hexAsBigInteger())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
            = layoutInflater?.inflate(R.layout.layout_safe_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        disposables += layout_safe_settings_swipe_refresh.refreshes()
                .map { true }
                .startWith(false)
                .flatMap {
                    viewModel.loadSafeInfo(it)
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSubscribe { showLoading(true) }
                            .doOnComplete { showLoading(false) }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(::updateInfo, ::handleError)

        disposables += layout_safe_settings_name_input.textChanges()
                .skipInitialValue()
                .debounce(500, TimeUnit.MILLISECONDS)
                // We need to map to string else distinctUntilChanged will not work
                .map { it.toString() }
                .distinctUntilChanged()
                .flatMapSingle {
                    viewModel.updateSafeName(it)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult({}, { errorSnackbar(layout_safe_settings_name_input, it) })

        disposables += viewModel.loadSafeName()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    layout_safe_settings_name_input.isEnabled = false
                }
                .doAfterTerminate {
                    layout_safe_settings_name_input.isEnabled = true
                }
                .subscribe({
                    layout_safe_settings_name_input.setText(it)
                    layout_safe_settings_name_input.setSelection(it.length)
                }, Timber::e)

        disposables += layout_safe_settings_delete_button.clicks()
                .subscribe({ showRemoveDialog() }, Timber::e)

        disposables += removeSafeClicks
                .flatMapSingle { viewModel.deleteSafe() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(onNext = { onSafeRemoved() }, onError = ::onSafeRemoveError)
    }

    private fun safeNameOrPlaceHolder(@StringRes placeholderRef: Int): String {
        val name = layout_safe_settings_name_input.text.toString()
        if (name.isBlank()) {
            return getString(placeholderRef)
        }
        return name
    }

    private fun onSafeRemoved() {
        context!!.toast(getString(R.string.safe_remove_success, safeNameOrPlaceHolder(R.string.safe)))
        startActivity(SafesOverviewActivity.createIntent(context!!).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }

    private fun onSafeRemoveError(throwable: Throwable) {
        snackbar(layout_safe_settings_swipe_refresh, R.string.safe_remove_error)
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
                .applicationComponent(component)
                .viewModule(ViewModule(context!!))
                .build().inject(this)
    }

    private fun showRemoveDialog() {
        AlertDialog.Builder(context!!)
                .setTitle(R.string.remove_safe_dialog_title)
                .setMessage(getString(R.string.remove_safe_dialog_description, safeNameOrPlaceHolder(R.string.this_safe)))
                .setPositiveButton(R.string.remove, { _, _ -> removeSafeClicks.onNext(Unit) })
                .setNegativeButton(R.string.cancel, { _, _ -> })
                .show()
    }

    private fun showLoading(loading: Boolean) {
        layout_safe_settings_swipe_refresh.isRefreshing = loading
    }

    private fun handleError(throwable: Throwable) {
        view?.let { errorSnackbar(it, throwable) }
    }

    private fun updateInfo(info: SafeInfo) {
        layout_safe_settings_confirmations.text = context!!.getString(R.string.safe_confirmations_text, info.requiredConfirmations.toString(), info.owners.size.toString())

        setupOwners(info.owners)
    }

    private fun setupOwners(owners: List<String>) {
        layout_safe_settings_owners_container.removeAllViews()
        owners.forEach { addOwner(it) }
    }

    private fun addOwner(address: String) {
        val ownerLayout = layoutInflater.inflate(R.layout.layout_address_item, layout_safe_settings_owners_container, false)
        ownerLayout.layout_address_item_value.text = address
        ownerLayout.layout_address_item_icon.setOnClickListener {
            SimpleAddressShareDialog.create(address).show(fragmentManager, null)
        }
        layout_safe_settings_owners_container.addView(ownerLayout)
    }

    companion object {
        private const val ARGUMENT_SAFE_ADDRESS = "argument.string.safe_address"

        fun createInstance(address: String) =
                SafeSettingsFragment().withArgs(
                        Bundle().build { putString(ARGUMENT_SAFE_ADDRESS, address) }
                )
    }
}
