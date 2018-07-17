package pm.gnosis.heimdall.ui.safe.pending

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_pending_safe.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.dialogs.share.SimpleAddressShareDialog
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.svalinn.common.utils.shareExternalText
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.withArgs
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Inject


class PendingSafeFragment : BaseFragment() {
    @Inject
    lateinit var viewModel: PendingSafeContract

    @Inject
    lateinit var eventTracker: EventTracker

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder().applicationComponent(component).viewModule(ViewModule(context!!)).build().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setup(arguments?.getString(EXTRA_SAFE_ADDRESS) ?: "")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.layout_pending_safe, container, false)

    override fun onStart() {
        super.onStart()
        eventTracker.submit(Event.ScreenView(ScreenId.NEW_SAFE_AWAIT_FUNDS))

        disposables += viewModel.observeCreationInfo()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onCreationInfo, onError = {
                errorSnackbar(layout_pending_safe_qr_code_button, it)
            })

        disposables += viewModel.observeHasEnoughDeployBalance()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = { hasEnoughBalanceForDeploy() }, onError = Timber::e)

        disposables += layout_pending_safe_qr_code_button.clicks()
            .subscribeBy(onNext = {
                SimpleAddressShareDialog.create(layout_pending_safe_address.text.toString())
                    .show(activity!!.supportFragmentManager, null)
            }, onError = Timber::e)

        disposables += layout_pending_safe_share_button.clicks()
            .subscribeBy(onNext = {
                context!!.shareExternalText(layout_pending_safe_address.text.toString())
            }, onError = Timber::e)
    }

    private fun hasEnoughBalanceForDeploy() {
        startActivity(SafeMainActivity.createIntent(context!!, arguments?.getString(EXTRA_SAFE_ADDRESS)?.asEthereumAddress()))
    }

    private fun onCreationInfo(info: PendingSafeContract.CreationInfo) {
        layout_pending_safe_address.text = info.safeAddress
        info.paymentToken?.let {
            layout_pending_safe_amount_label.text = getString(R.string.pending_safe_deposit_value, it.displayString(info.paymentAmount))
        }
    }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"

        fun createInstance(safe: PendingSafe) = PendingSafeFragment().withArgs(Bundle().apply {
            putString(EXTRA_SAFE_ADDRESS, safe.address.asEthereumAddressString())
        })
    }
}
