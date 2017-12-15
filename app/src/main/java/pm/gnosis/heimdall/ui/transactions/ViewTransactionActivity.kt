package pm.gnosis.heimdall.ui.transactions

import android.content.Context
import android.content.Intent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.toast
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.models.Transaction
import pm.gnosis.models.TransactionParcelable
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject


class ViewTransactionActivity : BaseTransactionActivity() {

    override fun screenId() = ScreenId.VIEW_TRANSACTION

    @Inject
    lateinit var subViewModel: ViewTransactionContract

    // Used for disposables that exists from onCreate to onDestroy
    private val lifetimeDisposables = CompositeDisposable()

    override fun loadTransactionDetails() {
        if (!loadTransactionType(intent)) {
            handleError()
        }
    }

    private fun loadTransactionType(intent: Intent?): Boolean {
        intent ?: return false
        val transaction = intent.getParcelableExtra<TransactionParcelable>(EXTRA_TRANSACTION)?.transaction ?: return false
        val safe = intent.getStringExtra(EXTRA_SAFE)
        lifetimeDisposables += subViewModel.checkTransactionType(transaction)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    displayTransactionDetails(createDetailsFragment(safe, it, transaction, false))
                }, this::handleError)
        return true
    }

    private fun handleError(throwable: Throwable? = null) {
        throwable?.let { Timber.e(throwable) }
        toast(R.string.transaction_details_error)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifetimeDisposables.clear()
    }

    override fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(HeimdallApplication[this].component)
                .viewModule(ViewModule(this))
                .build()
                .inject(this)
    }


    companion object {

        private const val EXTRA_SAFE = "extra.string.safe"
        private const val EXTRA_TRANSACTION = "extra.parcelable.transaction"

        fun createIntent(context: Context, safeAddress: BigInteger?, transaction: Transaction) =
                Intent(context, ViewTransactionActivity::class.java).apply {
                    putExtra(EXTRA_SAFE, safeAddress?.asEthereumAddressString())
                    putExtra(EXTRA_TRANSACTION, transaction.parcelable())
                }
    }
}