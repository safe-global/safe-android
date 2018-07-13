package pm.gnosis.heimdall.ui.safe.recover.submit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_recovering_safe_submit.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.RecoveringSafe
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.svalinn.common.utils.withArgs
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.stringWithNoTrailingZeroes
import javax.inject.Inject

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.layout_recovering_safe_submit, container, false)

    override fun onStart() {
        super.onStart()
        layout_recovering_safe_submit_button.isEnabled = false
        layout_recovering_safe_submit_button.visible(true)
        layout_recovering_safe_submit_retry.isEnabled = false
        layout_recovering_safe_submit_retry.visible(false)
        val safeAddress = arguments?.getString(EXTRA_SAFE_ADDRESS)?.asEthereumAddress() ?: throw IllegalStateException("No safe address provided!")
        addressHelper.populateAddressInfo(
            layout_recovering_safe_submit_info_safe_address,
            layout_recovering_safe_submit_info_safe_name,
            layout_recovering_safe_submit_info_safe_image,
            safeAddress
        ).forEach { disposables += it }

        // Automatically load on start
        disposables += layout_recovering_safe_submit_retry.clicks().startWith(Unit)
            .doOnNext {
                layout_recovering_safe_submit_retry.isEnabled = false
                layout_recovering_safe_submit_button.isEnabled = false
            }
            .switchMapSingle { viewModel.loadRecoveryExecuteInfo(safeAddress).mapToResult() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = {
                layout_recovering_safe_submit_data_balance_value.text =
                        context!!.getString(R.string.x_ether, it.balance.toEther().stringWithNoTrailingZeroes())
                layout_recovering_safe_submit_data_fees_value.text =
                        "- ${context!!.getString(R.string.x_ether, Wei(it.gasCosts()).toEther().stringWithNoTrailingZeroes())}"
                layout_recovering_safe_submit_button.visible(true)
                layout_recovering_safe_submit_button.isEnabled = true
                layout_recovering_safe_submit_retry.visible(false)
            }, onError = {
                errorSnackbar(layout_recovering_safe_submit_button, it)
                layout_recovering_safe_submit_button.visible(false)
                layout_recovering_safe_submit_retry.isEnabled = true
                layout_recovering_safe_submit_retry.visible(true)
            })

        disposables += layout_recovering_safe_submit_button.clicks()
            .doOnNext {
                layout_recovering_safe_submit_retry.isEnabled = false
                layout_recovering_safe_submit_button.isEnabled = false
            }
            .switchMapSingle { viewModel.submitRecovery(safeAddress).mapToResult() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = {
                startActivity(SafeMainActivity.createIntent(context!!, it))
            }, onError = {
                errorSnackbar(layout_recovering_safe_submit_button, it)
                layout_recovering_safe_submit_button.isEnabled = true
                layout_recovering_safe_submit_retry.isEnabled = true
            })
    }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"

        fun createInstance(safe: RecoveringSafe) = RecoveringSafeSubmitFragment().withArgs(Bundle().apply {
            putString(EXTRA_SAFE_ADDRESS, safe.address.asEthereumAddressString())
        })
    }
}
