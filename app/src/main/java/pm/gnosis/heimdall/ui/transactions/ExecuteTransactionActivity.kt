package pm.gnosis.heimdall.ui.transactions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.v7.widget.Toolbar
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.include_gas_price_selection.*
import kotlinx.android.synthetic.main.layout_address_item.view.*
import kotlinx.android.synthetic.main.layout_view_transaction.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.*
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.helpers.GasPriceHelper
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.dialogs.share.RequestSignatureDialog
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsActivity
import pm.gnosis.heimdall.ui.security.unlock.UnlockActivity
import pm.gnosis.heimdall.utils.displayString
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.heimdall.utils.handleQrCodeActivityResult
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.utils.asEthereumAddressStringOrNull
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

class ExecuteTransactionActivity : ViewTransactionActivity() {

    override fun screenId() = ScreenId.VIEW_TRANSACTION

    @Inject
    lateinit var gasPriceHelper: GasPriceHelper

    private var cachedTransactionData: CachedTransactionData? = null
    private var credentialsConfirmed: Boolean = false
    private var pendingSignature: String? = null

    private val transactionInfoTransformer: ObservableTransformer<Pair<BigInteger?, Result<Transaction>>, Result<ViewTransactionContract.Info>> =
            ObservableTransformer { up: Observable<Pair<BigInteger?, Result<Transaction>>> ->
                up
                        .doOnNext { setUnknownTransactionInfo() }
                        .checkedFlatMap { safeAddress, transaction ->
                            viewModel.loadExecuteInfo(safeAddress, transaction)
                        }
            }

    private val displayTransactionInformation: ObservableTransformer<Result<ViewTransactionContract.Info>, Any> =
            ObservableTransformer {
                Observable.combineLatest(
                        // Transaction Data
                        it,
                        // Price data
                        gasPriceHelper.let {
                            it.setup(include_gas_price_selection_root_container)
                            it.observe()
                        }.startWith(ErrorResult(Exception())),
                        BiFunction { info: Result<ViewTransactionContract.Info>, prices: Result<Wei> -> info to prices }
                )
                        .observeOn(AndroidSchedulers.mainThread())
                        // Update displayed information
                        .doOnNext({ (info: Result<ViewTransactionContract.Info>, prices: Result<Wei>) -> handleInfoWithPrices(info, prices) })
                        // Pass on data for submit
                        .compose(submitTransformer)
            }

    private val submitTransformer: ObservableTransformer<Pair<Result<ViewTransactionContract.Info>, Result<Wei>>, *> =
            ObservableTransformer { up: Observable<Pair<Result<ViewTransactionContract.Info>, Result<Wei>>> ->
                // We combine the data with the submit button events
                up.switchMap { infoWithGasPrice ->
                    layout_view_transaction_submit_button.clicks().map { infoWithGasPrice }
                }
                        .doOnNext { (info, gasPrice) ->
                            (info as? DataResult)?.let {
                                cachedTransactionData = CachedTransactionData(it.data.selectedSafe, it.data.status.transaction, (gasPrice as? DataResult)?.data)
                                startActivityForResult(UnlockActivity.createConfirmIntent(this), REQUEST_CODE_CONFIRM_CREDENTIALS)
                            } ?: run {
                                cachedTransactionData = null
                            }
                        }
            }

    private val requestSignatures: ObservableTransformer<Result<ViewTransactionContract.Info>, Any> =
            ObservableTransformer {
                it.observeOn(AndroidSchedulers.mainThread()).switchMap { info ->
                    layout_view_transaction_add_signature_button.clicks().map { info }
                            .doOnNextForResult({
                                RequestSignatureDialog
                                        .create(it.status.transactionHash, it.status.transaction, it.selectedSafe)
                                        .show(supportFragmentManager, null)
                            })
                }
            }

    private val signaturePushes: ObservableTransformer<Result<ViewTransactionContract.Info>, Any> =
            ObservableTransformer {
                it.switchMap { info ->
                    (info as? DataResult)?.let {
                        viewModel.observePushSignature(it.data.selectedSafe, it.data.status.transaction)
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnNextForResult(onError = { errorSnackbar(layout_view_transaction_add_signature_button, it) })
                    } ?: Observable.empty()
                }
            }

    override fun transactionDataTransformer(): ObservableTransformer<Pair<BigInteger?, Result<Transaction>>, Any> =
            ObservableTransformer {
                // Load execution information (hash, transaction with correct nonce, owner info)
                it.compose(transactionInfoTransformer).publish {
                    // Split execution information stream into two sub-streams
                    Observable.merge(
                            // Combine execution information and price data to update the displayed information
                            it.compose(displayTransactionInformation),
                            // Use execution information to allow adding signatures
                            it.compose(requestSignatures),
                            // Use execution information and observe signatures
                            it.compose(signaturePushes)
                    )
                }
            }

    override fun layout() = R.layout.layout_view_transaction

    override fun toolbar(): Toolbar = layout_view_transaction_toolbar

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        credentialsConfirmed = requestCode == REQUEST_CODE_CONFIRM_CREDENTIALS && resultCode == Activity.RESULT_OK
        handleQrCodeActivityResult(requestCode, resultCode, data, {
            pendingSignature = it
        }, {
            pendingSignature = null
        })
    }

    override fun onStart() {
        super.onStart()
        setUnknownTransactionInfo()
        cachedTransactionData?.let {
            if (credentialsConfirmed) {
                disposables += submitTransaction(it.safeAddress, it.transaction, it.overrideGasPrice)
                        .subscribeForResult({
                            eventTracker.submit(Event.SubmittedTransaction())
                            startActivity(SafeDetailsActivity.createIntent(this, Safe(it), R.string.tab_title_transactions).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        }, { showErrorSnackbar(it) })
            } else {
                snackbar(layout_view_transaction_submit_button, R.string.please_confirm_credentials)
            }
        }
        cachedTransactionData = null
        credentialsConfirmed = false
        pendingSignature?.let {
            disposables += viewModel.addSignature(it)
                    .subscribeBy({
                        Timber.e(it)
                        errorSnackbar(layout_view_transaction_submit_button, it)
                    })
        }
        pendingSignature = null
    }

    override fun fragmentRegistered() {
        layout_view_transaction_progress_bar.visibility = View.GONE
    }

    private fun submitTransaction(safe: BigInteger, transaction: Transaction, gasOverride: Wei?) =
            viewModel.submitTransaction(safe, transaction, gasOverride)
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
        layout_view_transaction_progress_bar.visible(loading)
        layout_view_transaction_submit_button.isEnabled = !loading
        transactionInputEnabled(!loading)
    }

    private fun updateInfo(info: ViewTransactionContract.Info, estimatedFees: Wei?) {
        info.status.let {
            val availableConfirmations = (if (it.isOwner) 1 else 0) + info.signatures.size
            val leftConfirmations = Math.max(0, it.requiredConfirmation - availableConfirmations)
            layout_view_transaction_confirmations_hint_text.text = getString(R.string.confirm_transaction_hint, leftConfirmations.toString())
            layout_view_transaction_confirmations.text = getString(R.string.x_of_x_confirmations, availableConfirmations.toString(), it.requiredConfirmation.toString())
            setViewVisibilities(availableConfirmations >= it.requiredConfirmation)
        }
        layout_view_transaction_confirmations_addresses.removeAllViews()
        if (info.status.isOwner) {
            layout_view_transaction_confirmations_addresses.addView(buildSignerView(getString(R.string.this_device), null))
        }
        info.signatures.forEach {
            layout_view_transaction_confirmations_addresses.addView(buildSignerView(null, it.key))
        }
        if (estimatedFees != null) {
            layout_view_transaction_transaction_fee.text = estimatedFees.displayString(this)
        } else {
            setUnknownEstimate()
        }
    }

    private fun setViewVisibilities(canSubmit: Boolean, canSign: Boolean = !canSubmit) {
        // If we can submit show information for submitting
        layout_view_transaction_submit_button.visible(canSubmit)
        layout_view_transaction_confirmations_divider.visible(canSubmit)
        layout_view_transaction_transaction_fee_divider.visible(canSubmit)
        layout_view_transaction_transaction_fee_label.visible(canSubmit)
        layout_view_transaction_transaction_fee.visible(canSubmit)
        include_gas_price_selection_root_container.visible(canSubmit)
        // If we can sign show information for signing
        layout_view_transaction_add_signature_button.visible(canSign)
    }

    private fun buildSignerView(name: String?, address: BigInteger?) =
            layoutInflater.inflate(R.layout.layout_address_item, layout_view_transaction_confirmations_addresses, false)
                    .apply {
                        layout_address_item_value.text = address?.asEthereumAddressStringOrNull()
                        layout_address_item_value.visible(address != null)
                        layout_address_item_name.text = name
                        layout_address_item_name.visible(name != null)
                    }

    private fun setUnknownEstimate() {
        layout_view_transaction_transaction_fee.text = "-"
    }

    private fun setUnknownTransactionInfo() {
        setUnknownEstimate()
        layout_view_transaction_confirmations_hint_text.text = getString(R.string.confirm_transaction_hint, "-")
        layout_view_transaction_confirmations.text = getString(R.string.x_of_x_confirmations, "-", "-")
        setViewVisibilities(false, false)
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

        fun createIntent(context: Context, safeAddress: BigInteger?, transaction: Transaction) =
                Intent(context, ExecuteTransactionActivity::class.java).apply {
                    putExtras(createBundle(safeAddress, transaction))
                }
    }
}