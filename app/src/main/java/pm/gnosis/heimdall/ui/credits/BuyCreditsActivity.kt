package pm.gnosis.heimdall.ui.credits

import android.content.Context
import android.content.Intent
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_buy_credits.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.svalinn.common.utils.*
import timber.log.Timber


class BuyCreditsActivity : ViewModelActivity<BuyCreditsContract>() {

    override fun screenId() = ScreenId.BUY_CREDITS

    override fun layout() = R.layout.layout_buy_credits

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onStart() {
        super.onStart()
        disposables += layout_buy_credits_buy_button.clicks()
            .switchMapSingle { viewModel.buyCredits(this) }
            .subscribe()

        disposables += viewModel.loadBalance()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = {
                layout_buy_credits_balance.text = getString(R.string.credits_balance, it.toString())
            }, onError = {
                errorSnackbar(layout_buy_credits_balance, it)
                layout_buy_credits_balance.text = getString(R.string.credits_balance, "-")
            })

        disposables += viewModel.observeVoucherState()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::updateState, onError = Timber::e)

        disposables += layout_buy_credits_redeem_button.clicks()
            .switchMap {
                viewModel.redeemPendingVoucher()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::updateState, onError = Timber::e)
    }

    private fun updateState(state: BuyCreditsContract.VoucherState) =
        when(state) {
            BuyCreditsContract.VoucherState.Bought -> showRedeemButton()
            BuyCreditsContract.VoucherState.NoVoucher -> showBuyButton()
            BuyCreditsContract.VoucherState.Redeeming -> showProgress()
            BuyCreditsContract.VoucherState.AlreadyRedeemed -> {
                showBuyButton()
                snackbar(layout_buy_credits_redeem_progress, R.string.voucher_already_redeemed)
            }
            is BuyCreditsContract.VoucherState.Redeemed -> {
                showBuyButton()
                layout_buy_credits_balance.text = getString(R.string.credits_balance, state.balance.toString())
            }
            is BuyCreditsContract.VoucherState.Error -> {
                showRedeemButton()
                errorSnackbar(layout_buy_credits_redeem_progress, state.error)
            }
        }

    private fun showProgress() {
        layout_buy_credits_buy_button.visible(false)
        layout_buy_credits_redeem_button.visible(false)
        layout_buy_credits_redeem_progress.visible(true)
    }

    private fun showRedeemButton() {
        layout_buy_credits_buy_button.visible(false)
        layout_buy_credits_redeem_button.visible(true)
        layout_buy_credits_redeem_progress.visible(false)
    }

    private fun showBuyButton() {
        layout_buy_credits_buy_button.visible(true)
        layout_buy_credits_redeem_button.visible(false)
        layout_buy_credits_redeem_progress.visible(false)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, BuyCreditsActivity::class.java)
    }
}
