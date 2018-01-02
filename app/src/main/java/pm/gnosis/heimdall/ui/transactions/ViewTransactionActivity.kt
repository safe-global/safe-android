package pm.gnosis.heimdall.ui.transactions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.include_gas_price_selection.*
import kotlinx.android.synthetic.main.layout_view_transaction.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.*
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.helpers.GasPriceHelper
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsActivity
import pm.gnosis.heimdall.ui.security.unlock.UnlockActivity
import pm.gnosis.heimdall.ui.transactions.details.assets.ReviewAssetTransferDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.base.BaseTransactionDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.generic.CreateGenericTransactionDetailsFragment
import pm.gnosis.heimdall.utils.displayString
import pm.gnosis.models.Transaction
import pm.gnosis.models.TransactionParcelable
import pm.gnosis.models.Wei
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject


class ViewTransactionActivity : BaseTransactionActivity() {

    override fun screenId() = ScreenId.VIEW_TRANSACTION

    @Inject
    lateinit var viewModel: ViewTransactionContract

    @Inject
    lateinit var gasPriceHelper: GasPriceHelper

    // Used for disposables that exists from onCreate to onDestroy
    private val lifetimeDisposables = CompositeDisposable()

    private var cachedTransactionData: CachedTransactionData? = null
    private var credentialsConfirmed: Boolean = false

    private val transactionInfoTransformer: ObservableTransformer<Pair<BigInteger?, Result<Transaction>>, Result<ViewTransactionContract.Info>> =
            ObservableTransformer { up: Observable<Pair<BigInteger?, Result<Transaction>>> ->
                up
                        .doOnNext { setUnknownTransactionInfo() }
                        .flatMap { data ->
                            checkInfoAndPerform(data, { safeAddress, transaction ->
                                viewModel.loadTransactionInfo(safeAddress, transaction)
                            })
                        }
                        .observeOn(AndroidSchedulers.mainThread())
            }

    private val submitTransformer: ObservableTransformer<Pair<Result<ViewTransactionContract.Info>, Result<Wei>>, *> =
            ObservableTransformer { up: Observable<Pair<Result<ViewTransactionContract.Info>, Result<Wei>>> ->
                // We combine the data with the submit button events
                up.switchMap { infoWithGasPrice ->
                    layout_view_transaction_submit_button.clicks().map { infoWithGasPrice }
                }
                        .doOnNext { (info, gasPrice) ->
                            (info as? DataResult)?.let {
                                cachedTransactionData = CachedTransactionData(it.data.selectedSafe, it.data.transaction, (gasPrice as? DataResult)?.data)
                                startActivityForResult(UnlockActivity.createConformIntent(this), REQUEST_CODE_CONFIRM_CREDENTIALS)
                            } ?: run {
                                cachedTransactionData = null
                            }
                        }
            }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        credentialsConfirmed = requestCode == REQUEST_CODE_CONFIRM_CREDENTIALS && resultCode == Activity.RESULT_OK
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_view_transaction)

        setupToolbar(layout_view_transaction_toolbar, R.drawable.ic_close_24dp)
    }

    override fun onStart() {
        super.onStart()
        setUnknownTransactionInfo()
        cachedTransactionData?.let {
            if (credentialsConfirmed) {
                disposables += submitTransaction(it.safeAddress, it.transaction, it.overrideGasPrice)
                        .subscribe({
                            it.handle({
                                eventTracker.submit(Event.SubmittedTransaction())
                                startActivity(SafeDetailsActivity.createIntent(this, Safe(it), R.string.tab_title_transactions).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                            }, { showErrorSnackbar(it) })
                        }, Timber::e)
            } else {
                snackbar(layout_view_transaction_submit_button, R.string.please_confirm_credentials)
            }
        }
        cachedTransactionData = null
        credentialsConfirmed = false
    }

    override fun fragmentRegistered() {
        layout_view_transaction_progress_bar.visibility = View.GONE
    }

    override fun transactionDataTransformer(): ObservableTransformer<Pair<BigInteger?, Result<Transaction>>, Any> =
            ObservableTransformer {
                // Combine transaction and price data to update the displayed information
                Observable.combineLatest(
                        // Transaction Data
                        it.compose(transactionInfoTransformer),
                        // Price data
                        gasPriceHelper.observe(include_gas_price_selection_root_container),
                        BiFunction { info: Result<ViewTransactionContract.Info>, prices: Result<Wei> -> info to prices }
                )
                        // Update displayed information
                        .doOnNext({ (info: Result<ViewTransactionContract.Info>, prices: Result<Wei>) -> handleInfoWithPrices(info, prices) })
                        // Pass on data for submit
                        .compose(submitTransformer)
            }

    private fun submitTransaction(safe: BigInteger, transaction: Transaction, gasOverride: Wei?) =
            viewModel.submitTransaction(safe, transaction, gasOverride)
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { submittingTransaction(true) }
                    .doAfterTerminate { submittingTransaction(false) }
                    .toObservable()

    private fun handleInfoWithPrices(info: Result<ViewTransactionContract.Info>, prices: Result<Wei>) {
        info.handle({
            val estimate = it.estimate?.let {
                Wei(((prices as? DataResult)?.data ?: it.gasPrice).value * it.gasCosts)
            }
            updateInfo(it, estimate)
        }, ::handleInputError)
    }

    private fun submittingTransaction(loading: Boolean) {
        layout_view_transaction_progress_bar.visibility = if (loading) View.VISIBLE else View.GONE
        layout_view_transaction_submit_button.isEnabled = !loading
        transactionInputEnabled(!loading)
    }

    private fun updateInfo(info: ViewTransactionContract.Info, estimatedFees: Wei?) {
        info.status.let {
            val availableConfirmations = if (it.isOwner) 1 else 0
            val leftConfirmations = Math.max(0, it.requiredConfirmation - availableConfirmations)
            layout_view_transaction_confirmations_hint_text.text = getString(R.string.confirm_transaction_hint, leftConfirmations.toString())
            layout_view_transaction_confirmations.text = getString(R.string.x_of_x_confirmations, availableConfirmations.toString(), it.requiredConfirmation.toString())
            layout_view_transaction_submit_button.isEnabled = availableConfirmations >= it.requiredConfirmation
        }
        if (estimatedFees != null) {
            layout_view_transaction_transaction_fee.text = estimatedFees.displayString(this)
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

    private data class CachedTransactionData(val safeAddress: BigInteger, val transaction: Transaction, val overrideGasPrice: Wei?)

    companion object {
        private const val REQUEST_CODE_CONFIRM_CREDENTIALS = 2342

        private const val EXTRA_SAFE = "extra.string.safe"
        private const val EXTRA_TRANSACTION = "extra.parcelable.transaction"

        fun createIntent(context: Context, safeAddress: BigInteger?, transaction: Transaction) =
                Intent(context, ViewTransactionActivity::class.java).apply {
                    putExtra(EXTRA_SAFE, safeAddress?.asEthereumAddressString())
                    putExtra(EXTRA_TRANSACTION, transaction.parcelable())
                }
    }
}