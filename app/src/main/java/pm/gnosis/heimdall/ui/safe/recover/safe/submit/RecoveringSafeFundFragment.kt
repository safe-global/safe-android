package pm.gnosis.heimdall.ui.safe.recover.safe.submit

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
import kotlinx.android.synthetic.main.fragment_recovering_safe_fund.recovering_safe_toolbar_shadow as toolbarShadow
import kotlinx.android.synthetic.main.fragment_recovering_safe_fund.recovering_safe_scroll_view as scrollView
import kotlinx.android.synthetic.main.fragment_recovering_safe_fund.recovering_safe_qr_code_container as qrCodeContainer
import kotlinx.android.synthetic.main.fragment_recovering_safe_fund.recovering_safe_qr_image as qrImage
import kotlinx.android.synthetic.main.fragment_recovering_safe_fund.recovering_safe_qr_progress as qrProgress
import kotlinx.android.synthetic.main.fragment_recovering_safe_fund.recovering_safe_share_button as shareBtn
import kotlinx.android.synthetic.main.fragment_recovering_safe_fund.recovering_safe_safe_address as safeAddressTxt
import kotlinx.android.synthetic.main.fragment_recovering_safe_fund.recovering_safe_safe_image as safeImage
import kotlinx.android.synthetic.main.fragment_recovering_safe_fund.recovering_safe_safe_balance as safeBalanceTxt
import kotlinx.android.synthetic.main.fragment_recovering_safe_fund.recovering_safe_amount_needed as amountNeededTxt
import kotlinx.android.synthetic.main.fragment_recovering_safe_fund.recovering_safe_remainder as remainderTxt
import kotlinx.android.synthetic.main.fragment_recovering_safe_fund.recovering_safe_only_send_x as onlySendTxt
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.RecoveringSafe
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.*
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import java.math.RoundingMode
import javax.inject.Inject

class RecoveringSafeFundFragment : BaseFragment() {

    override fun inject(component: ApplicationComponent) =
        DaggerViewComponent.builder()
            .viewModule(ViewModule(context!!))
            .applicationComponent(component)
            .build().inject(this)

    @Inject
    lateinit var viewModel: RecoveringSafeContract

    @Inject
    lateinit var eventTracker: EventTracker

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_recovering_safe_fund, container, false)

    override fun onStart() {
        super.onStart()
        eventTracker.submit(Event.ScreenView(ScreenId.RECOVER_SAFE_AWAIT_FUNDS))

        disposables += toolbarHelper.setupShadow(toolbarShadow, scrollView)

        val safeAddress = arguments?.getString(EXTRA_SAFE_ADDRESS)?.asEthereumAddress() ?: throw IllegalStateException("No safe address provided!")
        disposables += viewModel.observeRecoveryInfo(safeAddress)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onRecoveryInfo, onError = {
                errorSnackbar(qrCodeContainer as View, it)
            })

        disposables += viewModel.checkRecoveryFunded(safeAddress)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = ::onRecoveryFunded, onError = Timber::e)

        disposables += qrImage.longClicks()
            .subscribeBy(onNext = {
                context!!.copyToClipboard(getString(R.string.share_address), safeAddressTxt.text.toString()) {
                    snackbar(pending_safe_safe_address, R.string.address_clipboard_success)
                }
            }, onError = Timber::e)

        disposables += shareBtn.clicks()
            .subscribeBy(onNext = {
                context!!.shareExternalText(pending_safe_safe_address.text.toString())
            }, onError = Timber::e)
    }

    private fun onRecoveryFunded(safeAddress: Solidity.Address) {
        startActivity(SafeMainActivity.createIntent(context!!, safeAddress))
    }

    private fun onRecoveryInfo(info: RecoveringSafeContract.RecoveryInfo) {

        val addressString = SpannableStringBuilder(info.safeAddress)
        addressString.setSpan(ForegroundColorSpan(context!!.getColorCompat(R.color.address_boundaries)), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        addressString.setSpan(
            ForegroundColorSpan(context!!.getColorCompat(R.color.address_boundaries)),
            addressString.length - 4,
            addressString.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        safeAddressTxt.text = addressString

        safeImage.setAddress(info.safeAddress.asEthereumAddress())

        info.paymentToken?.let {
            amountNeededTxt.text = it.token.displayString(info.paymentAmount, roundingMode = RoundingMode.UP)
            onlySendTxt.text = getString(R.string.please_send_x, it.token.symbol)
            safeBalanceTxt.text = it.token.displayString(it.balance!!, roundingMode = RoundingMode.UP)
            remainderTxt.text = it.token.displayString(info.paymentAmount - it.balance, roundingMode = RoundingMode.UP)
        }

        info.qrCode?.let {
            qrProgress.visible(false)
            qrImage.visible(true)
            qrImage.setImageBitmap(it)
        }
    }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"

        fun createInstance(safe: RecoveringSafe) = RecoveringSafeFundFragment().withArgs(Bundle().apply {
            putString(EXTRA_SAFE_ADDRESS, safe.address.asEthereumAddressString())
        })
    }
}
