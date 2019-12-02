package pm.gnosis.heimdall.ui.safe.recover.safe.submit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_recovering_safe_submit.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.RecoveringSafe
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.utils.InfoTipDialogBuilder
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.svalinn.common.utils.withArgs
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject
import kotlinx.android.synthetic.main.include_transfer_summary_final.transfer_data_fees_error as feesError
import kotlinx.android.synthetic.main.include_transfer_summary_final.transfer_data_fees_info as feesInfo
import kotlinx.android.synthetic.main.include_transfer_summary_final.transfer_data_fees_value as feesValue
import kotlinx.android.synthetic.main.include_transfer_summary_final.transfer_data_safe_balance_after_value as balanceAfterValue
import kotlinx.android.synthetic.main.include_transfer_summary_final.transfer_data_safe_balance_before_value as balanceBeforeValue
import kotlinx.android.synthetic.main.layout_recovering_safe_submit.recover_safe_swipe_to_refresh as swipeToRefresh

class RecoveringSafeSubmitFragment : BaseFragment() {

    override fun inject(component: ApplicationComponent) =
        DaggerViewComponent.builder()
            .applicationComponent(component)
            .viewModule(ViewModule(context!!))
            .build().inject(this)

    @Inject
    lateinit var addressHelper: AddressHelper

    @Inject
    lateinit var viewModel: RecoveringSafeContract

    @Inject
    lateinit var eventTracker: EventTracker

    protected lateinit var safe: Solidity.Address

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.layout_recovering_safe_submit, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recover_safe_bottom_panel.disabled = true

        safe = arguments?.getString(EXTRA_SAFE_ADDRESS)?.asEthereumAddress() ?: throw IllegalStateException("No safe address provided!")

        addressHelper.populateAddressInfo(
            recover_safe_submit_info_safe_address,
            recover_safe_submit_info_safe_name,
            recover_safe_submit_info_safe_image,
            safe
        ).forEach { disposables += it }

        swipeToRefresh.setOnRefreshListener {
            loadExecuteInfo()
        }

        feesInfo.setOnClickListener {
            InfoTipDialogBuilder.build(context!!, R.layout.dialog_network_fee, R.string.ok).show()
        }

    }

    override fun onStart() {
        super.onStart()
        eventTracker.submit(Event.ScreenView(ScreenId.RECOVER_SAFE_REVIEW))

        disposables += recover_safe_bottom_panel.forwardClicks
            .doOnNext {
                onLoading(true)
            }
            .switchMapSingle { viewModel.submitRecovery(safe).mapToResult() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = {
                startActivity(SafeMainActivity.createIntent(context!!, it))
            }, onError = {
                errorSnackbar(recover_safe_bottom_panel, it)
                onLoading(false)
            })
    }

    override fun onResume() {
        super.onResume()
        loadExecuteInfo()
    }

    private fun loadExecuteInfo() {
        onLoading(true)
        viewModel.loadRecoveryExecuteInfo(safe)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                {
                    onLoading(false)
                    errorSnackbar(recover_safe_bottom_panel, it)
                },
                {
                    balanceBeforeValue.text = it.paymentToken.displayString(it.balance)
                    balanceAfterValue.text = it.paymentToken.displayString(it.balance - it.paymentAmount)
                    feesValue.text = it.paymentToken.displayString(it.paymentAmount)
                    feesError.visible(!it.canSubmit)
                    recover_safe_bottom_panel.disabled = !it.canSubmit
                    onLoading(false)
                }
            )
    }

    private fun onLoading(isLoading: Boolean) {
        recover_safe_bottom_panel.disabled = isLoading
        recover_safe_progress_bar.visible(isLoading)
        swipeToRefresh.isRefreshing = false
        swipeToRefresh.isActivated = !isLoading
        swipeToRefresh.isEnabled = !isLoading
    }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"

        fun createInstance(safe: RecoveringSafe) = RecoveringSafeSubmitFragment().withArgs(Bundle().apply {
            putString(EXTRA_SAFE_ADDRESS, safe.address.asEthereumAddressString())
        })
    }
}
