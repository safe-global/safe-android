package pm.gnosis.heimdall.ui.safe.pairing

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.Observer
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.tokens.payment.PaymentTokensActivity
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.heimdall.utils.InfoTipDialogBuilder
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.heimdall.utils.underline
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import kotlinx.android.synthetic.main.include_transfer_summary.transfer_data_fees_error as feesError
import kotlinx.android.synthetic.main.include_transfer_summary.transfer_data_fees_info as feesInfo
import kotlinx.android.synthetic.main.include_transfer_summary.transfer_data_fees_value as feesValue
import kotlinx.android.synthetic.main.include_transfer_summary.transfer_data_safe_balance_after_value as balanceAfterValue
import kotlinx.android.synthetic.main.include_transfer_summary.transfer_data_safe_balance_before_value as balanceBeforeValue
import kotlinx.android.synthetic.main.screen_2fa_pairing_start.pairing_back_arrow as backArrow
import kotlinx.android.synthetic.main.screen_2fa_pairing_start.pairing_bottom_panel as bottomPanel
import kotlinx.android.synthetic.main.screen_2fa_pairing_start.pairing_progress_bar as progressBar
import kotlinx.android.synthetic.main.screen_2fa_pairing_start.pairing_swipe_to_refresh as swipeToRefresh
import kotlinx.android.synthetic.main.screen_2fa_pairing_start.pairing_title as toolbarTitle
import kotlinx.android.synthetic.main.screen_2fa_pairing_start.pairing_info as infoText
import kotlinx.android.synthetic.main.screen_2fa_pairing_start.pairing_image as infoImage
import kotlinx.android.synthetic.main.screen_2fa_pairing_start.pairing_coordinator

abstract class PairingStartActivity : ViewModelActivity<PairingStartContract>() {

    protected lateinit var safe: Solidity.Address

    override fun layout() = R.layout.screen_2fa_pairing_start

    override fun inject(component: ViewComponent) = viewComponent().inject(this)

    @StringRes
    abstract fun titleRes(): Int

    @DrawableRes
    abstract fun getImageRes(): Int

    @StringRes
    abstract fun textRes(): Int

    abstract fun onStartClicked()

    open fun onAuthenticatorInfoLoaded(info: AuthenticatorSetupInfo) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        safe = intent.getStringExtra(EXTRA_SAFE_ADDRESS)?.asEthereumAddress() ?: run {
            finish()
            return
        }

        toolbarTitle.text = getString(titleRes())
        infoText.text = getString(textRes())
        infoImage.setImageResource(getImageRes())

        bottomPanel.disabled = true

        viewModel.setup(safe)

        viewModel.observableState.observe(this, Observer {

            when (it) {

                is PairingStartContract.ViewUpdate.Balance -> {

                    if (it.sufficient) {
                        bottomPanel.disabled = false
                        feesError.visible(false)
                    } else {
                        bottomPanel.disabled = true
                        feesError.text = getString(R.string.insufficient_funds_please_add, it.tokenSymbol)
                        feesError.visible(true)
                    }
                    progressBar.visible(false)
                    balanceBeforeValue.text = it.balanceBefore
                    feesValue.text = it.fee.underline()
                    balanceAfterValue.text = it.balanceAfter
                }

                is PairingStartContract.ViewUpdate.Authenticator -> {
                    onAuthenticatorInfoLoaded(it.info)
                }

                is PairingStartContract.ViewUpdate.Error -> {
                    errorSnackbar(pairing_coordinator, it.error)
                }
            }

            onLoading(false)
        })

        swipeToRefresh.setOnRefreshListener {
            startEstimation()
        }

        backArrow.setOnClickListener {
            finish()
        }

        feesValue.setOnClickListener {
            startActivity(PaymentTokensActivity.createIntent(this, safe))
        }

        feesInfo.setOnClickListener {
            InfoTipDialogBuilder.build(this, R.layout.dialog_network_fee, R.string.ok).show()
        }
    }

    override fun onStart() {
        super.onStart()

        disposables += bottomPanel.forwardClicks.subscribeBy {
            onStartClicked()
        }
    }

    override fun onResume() {
        super.onResume()
        startEstimation()
    }

    private fun startEstimation() {
        onLoading(true)
        viewModel.estimate()
    }

    private fun onLoading(isLoading: Boolean) {
        bottomPanel.disabled = isLoading
        progressBar.visible(isLoading)
        swipeToRefresh.isRefreshing = false
        swipeToRefresh.isActivated = !isLoading
        swipeToRefresh.isEnabled = !isLoading
    }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"

    }
}
