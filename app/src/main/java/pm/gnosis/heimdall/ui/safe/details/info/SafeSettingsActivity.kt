package pm.gnosis.heimdall.ui.safe.details.info

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v7.app.AlertDialog
import com.jakewharton.rxbinding2.support.v4.widget.refreshes
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_additional_owner_item.view.*
import kotlinx.android.synthetic.main.layout_safe_settings.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.snackbar
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.common.utils.toast
import pm.gnosis.heimdall.common.utils.visible
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.addressbook.helpers.AddressInfoViewHolder
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.base.InflatingViewProvider
import pm.gnosis.heimdall.ui.dialogs.transaction.CreateChangeSafeSettingsTransactionProgressDialog
import pm.gnosis.heimdall.ui.safe.overview.SafesOverviewActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.utils.hexAsEthereumAddressOrNull
import timber.log.Timber
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SafeSettingsActivity : BaseActivity() {

    @Inject
    lateinit var viewModel: SafeSettingsContract

    private val removeSafeClicks = PublishSubject.create<Unit>()

    private val viewProvider by lazy { InflatingViewProvider(layoutInflater, layout_safe_settings_owners_container, R.layout.layout_additional_owner_item) }

    override fun screenId() = ScreenId.SAFE_SETTINGS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_safe_settings)
        registerToolbar(layout_safe_settings_toolbar)

        intent.extras.getString(EXTRA_SAFE_ADDRESS).hexAsEthereumAddressOrNull()?.let {
            viewModel.setup(it)
        } ?: finish()
    }

    override fun onStart() {
        super.onStart()
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
                .doOnSubscribe { layout_safe_settings_name_input.isEnabled = false }
                .doAfterTerminate { layout_safe_settings_name_input.isEnabled = true }
                .subscribeBy(onSuccess = {
                    layout_safe_settings_toolbar.title = it
                    layout_safe_settings_name_input.setText(it)
                    layout_safe_settings_name_input.setSelection(it.length)
                }, onError = Timber::e)

        disposables += layout_safe_settings_delete_button.clicks()
                .subscribeBy(onNext = { showRemoveDialog() }, onError = Timber::e)

        disposables += removeSafeClicks
                .flatMapSingle { viewModel.deleteSafe() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(onNext = { onSafeRemoved() }, onError = ::onSafeRemoveError)
    }

    private fun safeNameOrPlaceHolder(@StringRes placeholderRef: Int): String {
        val name = layout_safe_settings_name_input.text.toString()
        return if (name.isBlank()) getString(placeholderRef) else name
    }

    private fun onSafeRemoved() {
        toast(getString(R.string.safe_remove_success, safeNameOrPlaceHolder(R.string.safe)))
        startActivity(SafesOverviewActivity.createIntent(this).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }

    private fun onSafeRemoveError(throwable: Throwable) {
        snackbar(layout_safe_settings_swipe_refresh, R.string.safe_remove_error)
    }

    private fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(HeimdallApplication[this].component)
                .viewModule(ViewModule(this))
                .build().inject(this)
    }

    private fun showRemoveDialog() {
        AlertDialog.Builder(this)
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
        errorSnackbar(layout_safe_settings_coordinator, throwable)
    }

    private fun updateInfo(info: SafeInfo) {
        layout_safe_settings_confirmations.text = getString(R.string.safe_confirmations_text, info.requiredConfirmations.toString(), info.owners.size.toString())

        layout_safe_settings_owners_container.removeAllViews()
        val ownerCount = info.owners.size
        val showDelete = ownerCount > 1
        info.owners.forEachIndexed { index, address -> addOwner(address, index, ownerCount, showDelete) }

        layout_safe_settings_add_owner_button.visible(ownerCount < 3)
        layout_safe_settings_add_owner_button.setOnClickListener {
            CreateChangeSafeSettingsTransactionProgressDialog
                    .addOwner(viewModel.getSafeAddress(), info.owners.size)
                    .show(supportFragmentManager, null)
        }
    }

    private fun addOwner(address: BigInteger, index: Int, count: Int, showDelete: Boolean) {
        AddressInfoViewHolder(this, viewProvider).apply {
            bind(address)
            view.layout_additional_owner_delete_button.visible(showDelete)
            view.layout_additional_owner_delete_button.setOnClickListener {
                CreateChangeSafeSettingsTransactionProgressDialog
                        .removeOwner(viewModel.getSafeAddress(), address, index.toLong(), count)
                        .show(supportFragmentManager, null)
            }
            layout_safe_settings_owners_container.addView(view)
        }
    }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "argument.string.safe_address"

        fun createIntent(context: Context, address: String) =
                Intent(context, SafeSettingsActivity::class.java).apply {
                    putExtra(EXTRA_SAFE_ADDRESS, address)
                }
    }
}
