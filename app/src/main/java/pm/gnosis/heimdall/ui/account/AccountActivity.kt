package pm.gnosis.heimdall.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.PopupMenu
import com.jakewharton.rxbinding2.support.v4.widget.refreshes
import com.jakewharton.rxbinding2.support.v7.widget.itemClicks
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_account.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.dialogs.share.SimpleAddressShareDialog
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.heimdall.utils.format
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.common.utils.doOnNextForResult
import pm.gnosis.svalinn.common.utils.flatMapResult
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.ticker.data.repositories.models.Currency
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.isValidEthereumAddress
import pm.gnosis.utils.stringWithNoTrailingZeroes
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AccountActivity : ViewModelActivity<AccountContract>() {

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    private lateinit var popupMenu: PopupMenu

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun layout() = R.layout.layout_account

    override fun screenId() = ScreenId.ACCOUNT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        popupMenu = PopupMenu(this, layout_account_overflow).apply {
            inflate(R.menu.account_menu)
        }
    }

    override fun onStart() {
        super.onStart()
        disposables += toolbarHelper.setupShadow(layout_account_toolbar_shadow, layout_account_content_scroll)
        disposables += layout_account_back_button.clicks()
            .subscribeBy { onBackPressed() }

        disposables += layout_account_overflow.clicks()
            .subscribeBy { popupMenu.show() }

        disposables += popupMenu.itemClicks()
            .subscribeBy(onNext = {
                when (it.itemId) {
                    R.id.account_menu_share -> if (layout_account_address.text.toString().isValidEthereumAddress()) {
                        SimpleAddressShareDialog.create(layout_account_address.text.toString()).show(supportFragmentManager, null)
                    }
                }
            })

        disposables += viewModel.getAccountAddress()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(this::onAccountAddress, ::handleError)

        // We delay the initial value, else the progress bar is not visible
        disposables += layout_account_refresh_layout.refreshes()
            .subscribeOn(AndroidSchedulers.mainThread())
            .startWith(Observable.just(Unit).delay(100, TimeUnit.MILLISECONDS))
            // We need to switch the observing scheduler here because `delay` forces the
            // computation scheduler and we need the ui thread scheduler when subscribing in the
            // flap map, else the loading spinner is triggered on the wrong thread
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { accountBalance() }
            .observeOn(AndroidSchedulers.mainThread())
            // Update balance view
            .doOnNextForResult(onNext = ::onAccountBalance)
            .flatMapResult(mapper = { viewModel.loadFiatConversion(it) })
            .observeOn(AndroidSchedulers.mainThread())
            // Update Fiat view
            .doOnNextForResult(onNext = ::onFiat)
            .subscribeForResult(onError = ::handleError)
    }

    private fun onFiat(currency: Pair<BigDecimal, Currency>) {
        layout_account_fiat.text = getString(
            R.string.fiat_approximation_parentheses,
            currency.first.stringWithNoTrailingZeroes(), currency.second.getFiatSymbol()
        )
    }

    private fun accountBalance() = viewModel.getAccountBalance()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { onAccountBalanceLoading(true) }
        .doOnTerminate { onAccountBalanceLoading(false) }

    private fun onAccountAddress(account: Account) {
        layout_account_address.text = account.address.asEthereumAddressString()
        layout_account_blockies.setAddress(account.address)
    }

    private fun onAccountBalance(wei: Wei) {
        val formattedValue = wei.format(this)
        layout_account_balance.setText(formattedValue.first)
        layout_account_balance.setCurrencySymbol(formattedValue.second)
    }

    private fun onAccountBalanceLoading(isLoading: Boolean) {
        layout_account_refresh_layout.isRefreshing = isLoading
    }

    private fun handleError(throwable: Throwable) {
        errorSnackbar(layout_account_coordinator_layout, throwable)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, AccountActivity::class.java)
    }
}
