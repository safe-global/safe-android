package pm.gnosis.heimdall.ui.transactions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_view_transaction.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.setupToolbar
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.common.utils.toast
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.transactions.details.assets.CreateAssetTransferDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.assets.ReviewAssetTransferDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.base.BaseTransactionDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.generic.CreateGenericTransactionDetailsFragment
import pm.gnosis.heimdall.utils.displayString
import pm.gnosis.models.Transaction
import pm.gnosis.models.TransactionParcelable
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject


class ViewTransactionActivity : BaseTransactionActivity() {

    override fun screenId() = ScreenId.VIEW_TRANSACTION

    @Inject
    lateinit var viewModel: ViewTransactionContract

    // Used for disposables that exists from onCreate to onDestroy
    private val lifetimeDisposables = CompositeDisposable()
    private var currentInfo: Pair<BigInteger?, Transaction>? = null

    private val transactionInfoTransformer: ObservableTransformer<Pair<BigInteger?, Result<Transaction>>, *> =
            ObservableTransformer { up: Observable<Pair<BigInteger?, Result<Transaction>>> ->
                up
                        .doOnNext { setUnknownTransactionInfo() }
                        .flatMap {
                            checkInfoAndPerform(it, { safeAddress, transaction ->
                                viewModel.loadTransactionInfo(safeAddress, transaction)
                            })
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext({ it.handle(::updateInfo, ::handleInputError) })
            }

    private val cacheTransactionInfoTransformer: ObservableTransformer<Pair<BigInteger?, Result<Transaction>>, *> =
            ObservableTransformer { up: Observable<Pair<BigInteger?, Result<Transaction>>> ->
                up
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext({ (safeAddress, transaction) ->
                            transaction.handle({ currentInfo = safeAddress to it }, { currentInfo = null })
                        })
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_view_transaction)

        setupToolbar(layout_view_transaction_toolbar, R.drawable.ic_close_24dp)
    }

    override fun onStart() {
        super.onStart()
        setUnknownTransactionInfo()
        disposables += layout_view_transaction_submit_button.clicks()
                .flatMap {
                    currentInfo?.let { (safeAddress, transaction) ->
                        safeAddress?.let {
                            viewModel.submitTransaction(safeAddress, transaction)
                                    .subscribeOn(AndroidSchedulers.mainThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .doOnSubscribe { submittingTransaction(true) }
                                    .doAfterTerminate { submittingTransaction(false) }
                                    .toObservable()
                        }
                    } ?: Observable.empty<Result<Unit>>()
                }
                .subscribeForResult({
                    eventTracker.submit(Event.SubmittedTransaction())
                    finish()
                }, { showErrorSnackbar(it) })
    }

    override fun fragmentRegistered() {
        layout_view_transaction_progress_bar.visibility = View.GONE
    }

    override fun handleTransactionData(observable: Observable<Pair<BigInteger?, Result<Transaction>>>) =
            Observable.merge(
                    observable.compose(transactionInfoTransformer),
                    observable.compose(cacheTransactionInfoTransformer)
            )!!

    private fun submittingTransaction(loading: Boolean) {
        layout_view_transaction_progress_bar.visibility = if (loading) View.VISIBLE else View.GONE
        layout_view_transaction_submit_button.isEnabled = !loading
        transactionInputEnabled(!loading)
    }

    private fun updateInfo(info: ViewTransactionContract.Info) {
        info.transactionInfo.let {
            val leftConfirmations = Math.max(0, it.requiredConfirmation - it.confirmations - if (it.isOwner) 1 else 0)
            layout_view_transaction_confirmations_hint_text.text = getString(R.string.confirm_transaction_hint, leftConfirmations.toString())
            layout_view_transaction_confirmations.text = getString(R.string.x_of_x_confirmations, it.confirmations.toString(), it.requiredConfirmation.toString())
            layout_view_transaction_submit_button.isEnabled = it.isOwner && !it.isExecuted
        }
        if (info.estimation != null) {
            layout_view_transaction_transaction_fee.text = info.estimation.displayString(this)
        } else {
            setUnknownEstimate()
        }
    }

    private fun setUnknownEstimate() {
        layout_view_transaction_transaction_fee.text = "-"
    }

    private fun setUnknownTransactionInfo() {
        setUnknownEstimate()
        layout_view_transaction_submit_button.isEnabled = false
        layout_view_transaction_confirmations_hint_text.text = getString(R.string.confirm_transaction_hint, "-")
        layout_view_transaction_confirmations.text = getString(R.string.x_of_x_confirmations, "-", "-")
    }

    override fun loadTransactionDetails() {
        if (!loadTransactionType(intent)) {
            handleError()
        }
    }

    private fun loadTransactionType(intent: Intent?): Boolean {
        intent ?: return false
        val transaction = intent.getParcelableExtra<TransactionParcelable>(EXTRA_TRANSACTION)?.transaction ?: return false
        val safe = intent.getStringExtra(EXTRA_SAFE)
        lifetimeDisposables += viewModel.checkTransactionType(transaction)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    displayTransactionDetails(createDetailsFragment(safe, it, transaction))
                }, this::handleError)
        return true
    }

    override fun createDetailsFragment(safeAddress: String?, type: TransactionType, transaction: Transaction?): BaseTransactionDetailsFragment =
            when (type) {
                TransactionType.TOKEN_TRANSFER, TransactionType.ETHER_TRANSFER ->
                    ReviewAssetTransferDetailsFragment.createInstance(transaction, safeAddress)
                else -> CreateGenericTransactionDetailsFragment.createInstance(transaction, safeAddress, false)
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