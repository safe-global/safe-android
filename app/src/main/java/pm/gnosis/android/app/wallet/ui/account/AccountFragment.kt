package pm.gnosis.android.app.wallet.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_account.*
import pm.gnosis.android.app.wallet.R
import pm.gnosis.android.app.wallet.data.model.Wei
import pm.gnosis.android.app.wallet.di.component.ApplicationComponent
import pm.gnosis.android.app.wallet.di.component.DaggerViewComponent
import pm.gnosis.android.app.wallet.di.module.ViewModule
import pm.gnosis.android.app.wallet.ui.base.BaseFragment
import pm.gnosis.android.app.wallet.util.copyToClipboard
import pm.gnosis.android.app.wallet.util.shareExternalText
import pm.gnosis.android.app.wallet.util.snackbar
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

class AccountFragment : BaseFragment() {
    @Inject lateinit var presenter: AccountPresenter

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_account, container, false)
    }

    override fun onStart() {
        super.onStart()
        disposables += accountBalanceDisposable()
        fragment_account_address.text = presenter.getAccountAddress()

        fragment_account_clipboard.setOnClickListener {
            context.copyToClipboard("address", fragment_account_address.text.toString())
            snackbar(fragment_account_coordinator_layout, "Copied address to clipboard")
        }

        fragment_account_share.setOnClickListener {
            context.shareExternalText(fragment_account_address.text.toString(), "Share your address")
        }
    }

    private fun accountBalanceDisposable() = presenter.getAccountBalance()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { onAccountBalanceLoading(true) }
            .doOnTerminate { onAccountBalanceLoading(isLoading = false) }
            .subscribeBy(onNext = this::onAccountBalance, onError = this::onAccountBalanceError)

    private fun onAccountBalance(wei: Wei) {
        val etherBalance = wei.toEther()
        fragment_account_balance.text = if (etherBalance.compareTo(BigDecimal.ZERO) == 0) "0 Ξ" else etherBalance.stripTrailingZeros().toPlainString() + " Ξ"
    }

    private fun onAccountBalanceLoading(isLoading: Boolean) {
        progress_bar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun onAccountBalanceError(throwable: Throwable) {
        Timber.e(throwable)
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
                .applicationComponent(component)
                .viewModule(ViewModule(this.context))
                .build()
                .inject(this)
    }
}
