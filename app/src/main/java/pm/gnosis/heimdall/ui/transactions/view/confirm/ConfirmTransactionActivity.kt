package pm.gnosis.heimdall.ui.transactions.view.confirm


import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.widget.NestedScrollView
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_confirm_transaction.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.NfcViewModelActivity
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.security.unlock.UnlockDialog
import pm.gnosis.heimdall.ui.transactions.view.TransactionInfoViewHolder
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper.Events
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper.ViewUpdate
import pm.gnosis.heimdall.ui.transactions.view.helpers.TransactionSubmitInfoViewHelper
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexAsBigIntegerOrNull
import pm.gnosis.utils.nullOnThrow
import pm.gnosis.utils.toHexString
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

class ConfirmTransactionActivity : NfcViewModelActivity<ConfirmTransactionContract>(), UnlockDialog.UnlockCallback {

    @Inject
    lateinit var infoViewHelper: TransactionSubmitInfoViewHelper

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    private var transactionInfoViewHolder: TransactionInfoViewHolder? = null

    private lateinit var transaction: SafeTransaction

    private val unlockStatusSubject = PublishSubject.create<Unit>()

    override fun screenId() = ScreenId.INCOMING_TRANSACTION_REVIEW

    override fun layout() = R.layout.layout_confirm_transaction

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onUnlockSuccess(requestCode: Int) {
        if (requestCode == REQUEST_CODE_VERIFY_REJECT) {
            disposables +=
                    viewModel.rejectTransaction(transaction)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe {
                            infoViewHelper.toggleReadyState(false, R.string.rejecting_transaction)
                        }
                        .doAfterTerminate {
                            infoViewHelper.toggleReadyState(true)
                        }
                        .subscribeBy(onComplete = { finish() }, onError = ::dataError)
        } else {
            unlockStatusSubject.onNext(Unit)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val safeAddress = intent.getStringExtra(EXTRA_SAFE_ADDRESS)?.asEthereumAddress()
        val hash = intent.getStringExtra(EXTRA_TRANSACTION_HASH)
        val operationalGas = intent.getStringExtra(EXTRA_OPERATIONAL_GAS)?.hexAsBigIntegerOrNull()
        val dataGas = intent.getStringExtra(EXTRA_DATA_GAS)?.hexAsBigIntegerOrNull()
        val txGas = intent.getStringExtra(EXTRA_TX_GAS)?.hexAsBigIntegerOrNull()
        val gasToken = intent.getStringExtra(EXTRA_GAS_TOKEN)?.asEthereumAddress()
        val gasPrice = intent.getStringExtra(EXTRA_GAS_PRICE)?.hexAsBigIntegerOrNull()
        val signature = intent.getStringExtra(EXTRA_SIGNATURE)?.let { nullOnThrow { Signature.from(it) } }

        if (
            safeAddress == null || hash == null || signature == null ||
            operationalGas == null || dataGas == null || txGas == null || gasToken == null || gasPrice == null
        ) {
            finish()
            return
        }


        transaction = intent.getParcelableExtra(EXTRA_TRANSACTION) ?: run {
            finish()
            return
        }

        viewModel.setup(safeAddress, hash, operationalGas, dataGas, txGas, gasToken, gasPrice, transaction.wrapped.nonce, signature)
        infoViewHelper.bind(layout_confirm_transaction_transaction_info)
    }

    override fun onStart() {
        super.onStart()

        infoViewHelper.onToggleReadyState = this::toggleReadyState
        infoViewHelper.toggleRejectionState(false)
        infoViewHelper.resetConfirmationViews()
        infoViewHelper.toggleReadyState(false)
        layout_confirm_transaction_transaction_info.visible(true)
        layout_confirm_transaction_loading_error_group.visible(false)

        val submitEvents = unlockStatusSubject
            .doOnNext {
                infoViewHelper.toggleReadyState(false, R.string.submitting_transaction)
            }

        disposables += layout_confirm_transaction_confirm_button.clicks()
            .subscribeBy(onNext = {
                UnlockDialog.create().show(supportFragmentManager, null)
            })

        disposables += layout_confirm_transaction_reject_button.clicks()
            .subscribeBy(onNext = {
                UnlockDialog.create(REQUEST_CODE_VERIFY_REJECT).show(supportFragmentManager, null)
            })

        val events = Events(infoViewHelper.retryEvents(), infoViewHelper.requestConfirmationEvents(), submitEvents)
        disposables += viewModel.observe(events, transaction)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::applyUpdate, onError = this::dataError)

        disposables += layout_confirm_transaction_back_button.clicks()
            .subscribeBy { onBackPressed() }


        (layout_confirm_transaction_transaction_info as? NestedScrollView)?.let {
            disposables += toolbarHelper.setupShadow(layout_confirm_transaction_toolbar_shadow, it)
        }
    }

    private fun applyUpdate(update: ViewUpdate) {
        when (update) {
            is ViewUpdate.TransactionInfo ->
                setupViewHolder(update.viewHolder)
            is ViewUpdate.TransactionSubmitted -> {
                if (update.txHash != null) {
                    finish()
                } else {
                    infoViewHelper.toggleReadyState(true)
                }
            }
            else ->
                infoViewHelper.applyUpdate(update)?.let { disposables += it }
        }
    }

    private fun toggleReadyState(isReady: Boolean) {
        layout_confirm_transaction_confirm_button.visible(isReady)
        layout_confirm_transaction_reject_button.visible(isReady)
    }

    private fun setupViewHolder(viewHolder: TransactionInfoViewHolder) {
        // We already display this view holder no need to update
        if (viewHolder == transactionInfoViewHolder) return
        // Cleanup previous view holder
        transactionInfoViewHolder?.let {
            lifecycle.removeObserver(it)
            it.detach()
        }

        transactionInfoViewHolder = viewHolder

        infoViewHelper.setupViewHolder(layoutInflater, viewHolder)

        // Register new view holder
        lifecycle.addObserver(viewHolder)
    }

    private fun dataError(throwable: Throwable) {
        Timber.e(throwable)
        errorSnackbar(layout_confirm_transaction_transaction_info, throwable)
        val errorMsgId = (throwable as? ConfirmTransactionContract.InvalidTransactionException)?.messageId ?: R.string.error_loading_transaction
        layout_confirm_transaction_loading_error_message.text = getString(errorMsgId)
        layout_confirm_transaction_loading_error_group.visible(transactionInfoViewHolder == null)
    }

    companion object {
        private const val REQUEST_CODE_VERIFY_REJECT = 666
        private const val EXTRA_SIGNATURE = "extra.string.signature"
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"
        private const val EXTRA_TRANSACTION = "extra.parcelable.transaction"
        private const val EXTRA_TRANSACTION_HASH = "extra.string.transaction_hash"
        private const val EXTRA_OPERATIONAL_GAS = "extra.string.operational_gas"
        private const val EXTRA_DATA_GAS = "extra.string.data_gas"
        private const val EXTRA_TX_GAS = "extra.string.tx_gas"
        private const val EXTRA_GAS_TOKEN = "extra.string.gas_token"
        private const val EXTRA_GAS_PRICE = "extra.string.gas_price"
        fun createIntent(
            context: Context, signature: Signature, safe: Solidity.Address, transaction: SafeTransaction, hash: String,
            operationalGas: BigInteger, dataGas: BigInteger, txGas: BigInteger, gasToken: Solidity.Address, gasPrice: BigInteger
        ) =
            Intent(context, ConfirmTransactionActivity::class.java).apply {
                putExtra(EXTRA_SAFE_ADDRESS, safe.value.toHexString())
                putExtra(EXTRA_SIGNATURE, signature.toString())
                putExtra(EXTRA_TRANSACTION, transaction)
                putExtra(EXTRA_TRANSACTION_HASH, hash)
                putExtra(EXTRA_OPERATIONAL_GAS, operationalGas.toHexString())
                putExtra(EXTRA_DATA_GAS, dataGas.toHexString())
                putExtra(EXTRA_TX_GAS, txGas.toHexString())
                putExtra(EXTRA_GAS_TOKEN, gasToken.value.toHexString())
                putExtra(EXTRA_GAS_PRICE, gasPrice.toHexString())
            }
    }
}
