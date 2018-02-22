package pm.gnosis.heimdall.ui.transactions

import android.app.Activity
import android.arch.lifecycle.LifecycleOwner
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
import kotlinx.android.synthetic.main.layout_submit_transaction.*
import kotlinx.android.synthetic.main.layout_transaction_confirmation_item.view.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.*
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.helpers.GasPriceHelper
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.addressbook.helpers.AddressInfoViewHolder
import pm.gnosis.heimdall.ui.base.InflatingViewProvider
import pm.gnosis.heimdall.ui.dialogs.share.RequestSignatureDialog
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsActivity
import pm.gnosis.heimdall.ui.security.unlock.UnlockActivity
import pm.gnosis.heimdall.utils.displayString
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.heimdall.utils.handleQrCodeActivityResult
import pm.gnosis.heimdall.utils.setFormattedText
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Provider

class SubmitTransactionActivity : ViewTransactionActivity() {

    private val viewProvider by lazy { InflatingViewProvider(layoutInflater, layout_submit_transaction_confirmations_addresses, R.layout.layout_transaction_confirmation_item) }

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
                    layout_submit_transaction_submit_button.clicks().map { infoWithGasPrice }
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

    private val requestSignaturesQR: ObservableTransformer<Result<ViewTransactionContract.Info>, Any> =
            ObservableTransformer {
                it.observeOn(AndroidSchedulers.mainThread()).switchMap { info ->
                    layout_submit_transaction_scan_signature_qr_button.clicks().map { info }
                            .doOnNextForResult({
                                RequestSignatureDialog
                                        .create(it.status.transactionHash, it.status.transaction, it.selectedSafe)
                                        .show(supportFragmentManager, null)
                            })
                }
            }

    private val requestSignaturesPush: ObservableTransformer<Result<ViewTransactionContract.Info>, Result<Unit>> =
            ObservableTransformer {
                it.observeOn(AndroidSchedulers.mainThread()).switchMap { info ->
                    layout_submit_transaction_send_signature_push_button.clicks().map { info }
                }
                        .flatMapResult(::requestSignatureViaPush)
                        .doOnNextForResult(
                                onNext = {
                                    snackbar(layout_submit_transaction_send_signature_push_button, R.string.signature_request_sent)
                                },
                                onError = {
                                    errorSnackbar(layout_submit_transaction_send_signature_push_button, it)
                                }
                        )
            }

    private val signaturePushes: ObservableTransformer<Result<ViewTransactionContract.Info>, Any> =
            ObservableTransformer {
                it.switchMap { info ->
                    (info as? DataResult)?.let {
                        viewModel.observeSignaturePushes(it.data.selectedSafe, it.data.status.transaction)
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnNextForResult(onError = { errorSnackbar(layout_submit_transaction_send_signature_push_button, it) })
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
                            it.compose(requestSignaturesQR),
                            it.compose(requestSignaturesPush),
                            // Use execution information and observe signatures
                            it.compose(signaturePushes)
                    )
                }
            }

    override fun screenId() = ScreenId.SUBMIT_TRANSACTION

    override fun layout() = R.layout.layout_submit_transaction

    override fun toolbar(): Toolbar = layout_submit_transaction_toolbar

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
                snackbar(layout_submit_transaction_submit_button, R.string.please_confirm_credentials)
            }
        }
        cachedTransactionData = null
        credentialsConfirmed = false
        pendingSignature?.let {
            disposables += viewModel.addSignature(it)
                    .subscribeBy({
                        Timber.e(it)
                        errorSnackbar(layout_submit_transaction_submit_button, it)
                    })
        }
        pendingSignature = null
    }

    override fun fragmentRegistered() {
        layout_submit_transaction_progress_bar.visibility = View.GONE
    }

    private fun requestSignatureViaPush(info: ViewTransactionContract.Info) =
            viewModel.sendSignaturePush(info)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe {
                        layout_submit_transaction_send_signature_push_button.isEnabled = false
                        layout_submit_transaction_send_signature_push_progress.visible(true)
                    }
                    .doAfterTerminate {
                        layout_submit_transaction_send_signature_push_button.isEnabled = true
                        layout_submit_transaction_send_signature_push_progress.visible(false)
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
        layout_submit_transaction_progress_bar.visible(loading)
        layout_submit_transaction_submit_button.isEnabled = !loading
        transactionInputEnabled(!loading)
    }

    private fun updateInfo(info: ViewTransactionContract.Info, estimatedFees: Wei?) {
        val availableConfirmations = (if (info.status.isOwner) 1 else 0) + info.signatures.size
        val requiredConfirmationsAvailable = availableConfirmations >= info.status.requiredConfirmation
        info.status.let {
            val leftConfirmations = Math.max(0, it.requiredConfirmation - availableConfirmations)

            if (leftConfirmations > 0) {
                layout_submit_transaction_confirmations_hint_text.setFormattedText(R.string.confirm_transaction_hint, "required_confirmations" to leftConfirmations.toString())
            } else {
                layout_submit_transaction_confirmations_hint_text.setText(R.string.submit_transaction_hint)
            }
            layout_submit_transaction_confirmations.text = getString(R.string.x_of_x_confirmations, availableConfirmations.toString(), it.requiredConfirmation.toString())
            setViewStates(requiredConfirmationsAvailable)
        }
        var childIndex = 0
        // Add current device first
        if (info.status.isOwner) {
            addSignerView(childIndex++, info.status.sender, false, getString(R.string.this_device))
        }
        // Add confirmed devices
        info.status.owners.forEach {
            if (it != info.status.sender && info.signatures.containsKey(it)) {
                addSignerView(childIndex++, it, false)
            }
        }
        // Add pending devices if more confirmations are required
        if (!requiredConfirmationsAvailable) {
            info.status.owners.forEach {
                if (it != info.status.sender && !info.signatures.containsKey(it)) {
                    addSignerView(childIndex++, it, true)
                }
            }
        }
        (childIndex until layout_submit_transaction_confirmations_addresses.childCount).forEach {
            layout_submit_transaction_confirmations_addresses.removeViewAt(it)
        }

        if (estimatedFees != null) {
            layout_submit_transaction_transaction_fee.text = estimatedFees.displayString(this)
        } else {
            setUnknownEstimate()
        }
    }

    private fun addSignerView(index: Int, address: BigInteger, pending: Boolean, name: String? = null) {
        val child = layout_submit_transaction_confirmations_addresses.getChildAt(index)
        val vh = child?.tag as? SignatureAddressInfoViewHolder
        if (vh?.currentAddress == address) {
            vh.bind(name, address, pending)
        } else {
            child?.let { layout_submit_transaction_confirmations_addresses.removeView(it) }
            SignatureAddressInfoViewHolder(this, viewProvider).apply {
                bind(name, address, pending)
                layout_submit_transaction_confirmations_addresses.addView(view)
            }
        }
    }

    private fun setViewStates(canSubmit: Boolean, canSign: Boolean = !canSubmit) {
        // If we can submit show information for submitting
        layout_submit_transaction_submit_button.isEnabled = canSubmit
        layout_submit_transaction_all_confirmed_hint.visible(canSubmit)
        // If we can sign show information for signing
        layout_submit_transaction_scan_signature_qr_button.visible(canSign)
        layout_submit_transaction_send_signature_push_button.visible(canSign)
    }

    private fun setUnknownEstimate() {
        layout_submit_transaction_transaction_fee.text = "-"
    }

    private fun setUnknownTransactionInfo() {
        setUnknownEstimate()
        layout_submit_transaction_confirmations_hint_text.setFormattedText(R.string.confirm_transaction_hint, "required_confirmations" to "-")
        layout_submit_transaction_confirmations.text = getString(R.string.x_of_x_confirmations, "-", "-")
        setViewStates(false, false)
    }

    override fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(HeimdallApplication[this].component)
                .viewModule(ViewModule(this))
                .build()
                .inject(this)
    }

    private class SignatureAddressInfoViewHolder(lifecycleOwner: LifecycleOwner, viewProvider: Provider<View>) :
            AddressInfoViewHolder(lifecycleOwner, viewProvider) {

        private var currentName: String? = null

        fun bind(name: String?, address: BigInteger, pending: Boolean) {
            currentName = currentName ?: name
            bind(address)
            view.apply {
                layout_address_item_value.visible(name == null)
                layout_address_item_name.text = currentName
                layout_address_item_name.visible(currentName != null)
                layout_transaction_confirmation_item_status.setImageResource(if (pending) R.drawable.ic_pending_signature else R.drawable.ic_approved_signature)
            }
        }

        override fun onAddressInfo(entry: AddressBookEntry) {
            super.onAddressInfo(entry)
            currentName = entry.name
        }

        override fun start() {
            if (currentName == null) {
                // If we don't have a preset name load
                super.start()
            }
        }
    }

    private data class CachedTransactionData(val safeAddress: BigInteger, val transaction: Transaction, val overrideGasPrice: Wei?)

    companion object {
        private const val REQUEST_CODE_CONFIRM_CREDENTIALS = 2342

        fun createIntent(context: Context, safeAddress: BigInteger?, transaction: Transaction) =
                Intent(context, SubmitTransactionActivity::class.java).apply {
                    putExtras(createBundle(safeAddress, transaction))
                }
    }
}