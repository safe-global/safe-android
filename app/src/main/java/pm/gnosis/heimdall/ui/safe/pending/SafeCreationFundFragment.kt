package pm.gnosis.heimdall.ui.safe.pending

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.longClicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_pending_safe.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.utils.InfoTipDialogBuilder
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.svalinn.common.utils.*
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import java.math.RoundingMode
import javax.inject.Inject

class SafeCreationFundFragment : BaseFragment() {
    @Inject
    lateinit var viewModel: SafeCreationFundContract

    @Inject
    lateinit var eventTracker: EventTracker

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder().applicationComponent(component).viewModule(ViewModule(context!!)).build().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setup(arguments?.getString(EXTRA_SAFE_ADDRESS) ?: "")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_pending_safe, container, false)

    override fun onStart() {
        super.onStart()
        eventTracker.submit(Event.ScreenView(ScreenId.NEW_SAFE_AWAIT_FUNDS))

        disposables += toolbarHelper.setupShadow(pending_safe_toolbar_shadow, pending_safe_scroll_view)

        disposables += viewModel.observeCreationInfo()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onCreationInfo, onError = {
                errorSnackbar(pending_safe_qr_code_container, it)
            })

        disposables += viewModel.observeHasEnoughDeployBalance()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = { hasEnoughBalanceForDeploy() }, onError = Timber::e)

        disposables += pending_safe_qr_image.longClicks()
            .subscribeBy(onNext = {
                context!!.copyToClipboard(getString(R.string.share_address), pending_safe_safe_address.text.toString()) {
                    snackbar(pending_safe_safe_address, R.string.address_clipboard_success)
                }
            }, onError = Timber::e)

        disposables += pending_safe_share_button.clicks()
            .subscribeBy(onNext = {
                context!!.shareExternalText(pending_safe_safe_address.text.toString())
            }, onError = Timber::e)
    }

    private fun hasEnoughBalanceForDeploy() {
        startActivity(SafeMainActivity.createIntent(context!!, arguments?.getString(EXTRA_SAFE_ADDRESS)?.asEthereumAddress()))
    }

    private fun onCreationInfo(info: SafeCreationFundContract.CreationInfo) {

        val addressString = SpannableStringBuilder(info.safeAddress)
        addressString.setSpan(ForegroundColorSpan(context!!.getColorCompat(R.color.address_boundaries)), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        addressString.setSpan(
            ForegroundColorSpan(context!!.getColorCompat(R.color.address_boundaries)),
            addressString.length - 4,
            addressString.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        pending_safe_safe_address.text = addressString

        pending_safe_safe_image.setAddress(info.safeAddress.asEthereumAddress())

        info.tokenWithBalance?.let {
            pending_safe_amount_needed.text = it.token.displayString(info.paymentAmount, roundingMode = RoundingMode.UP)

            pending_safe_only_send_x.text = getString(R.string.please_send_x, it.token.symbol)

            pending_safe_safe_balance.text = it.token.displayString(it.balance!!, roundingMode = RoundingMode.UP)

            pending_safe_remainder.text = it.token.displayString(info.paymentAmount - it.balance, roundingMode = RoundingMode.UP)
        }

        info.qrCode?.let {
            pending_safe_qr_progress.visible(false)
            pending_safe_qr_image.visible(true)
            pending_safe_qr_image.setImageBitmap(it)
        }

        disposables +=  pending_safe_fees_info.clicks()
            .subscribeBy {
                InfoTipDialogBuilder.build(context!!, R.layout.dialog_network_fee, R.string.ok).show()
            }
    }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"

        fun createInstance(safe: PendingSafe) = SafeCreationFundFragment().withArgs(Bundle().apply {
            putString(EXTRA_SAFE_ADDRESS, safe.address.asEthereumAddressString())
        })
    }
}
