package pm.gnosis.heimdall.ui.transactions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_receipt_transaction.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.data.repositories.TransactionRepository.PublishStatus
import pm.gnosis.heimdall.data.repositories.TransactionRepository.PublishStatus.*
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.transactions.details.assets.ReceiptAssetTransferDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.base.BaseTransactionDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.generic.CreateGenericTransactionDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.safe.ReceiptChangeSafeSettingsDetailsFragment
import pm.gnosis.heimdall.utils.setupEtherscanTransactionUrl
import pm.gnosis.heimdall.utils.setupToolbar
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

class ReceiptTransactionActivity : BaseTransactionActivity() {

    @Inject
    lateinit var viewModel: ReceiptTransactionContract

    // Used for disposables that exists from onCreate to onDestroy
    private val lifetimeDisposables = CompositeDisposable()

    override fun screenId() = ScreenId.RECEIPT_TRANSACTION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_receipt_transaction)

        setupToolbar(layout_receipt_transaction_toolbar)
    }

    override fun onStart() {
        super.onStart()
        val txId = intent.getStringExtra(EXTRA_TX_ID) ?: return
        disposables += viewModel.observeTransactionStatus(txId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(::updateStatus, Timber::e)

        disposables += viewModel.loadChainHash(txId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = {
                layout_receipt_transaction_url.setupEtherscanTransactionUrl(it, R.string.view_transaction_on)
            }, onError = { layout_receipt_transaction_url.setText(R.string.unknown_transaction_url) })
    }

    private fun updateStatus(status: PublishStatus) {
        layout_receipt_transaction_title.setText(
            when (status) {
                UNKNOWN, PENDING -> R.string.transaction_status_pending
                FAILED -> R.string.transaction_status_failed
                SUCCESS -> R.string.transaction_status_success
            }
        )
    }

    override fun transactionDataTransformer(): ObservableTransformer<Pair<BigInteger?, Result<Transaction>>, Any> =
        ObservableTransformer {
            it.map { Any() }
        }

    override fun fragmentRegistered() {
        layout_receipt_transaction_progress_bar.visibility = View.GONE
    }

    override fun loadTransactionDetails() {
        if (!loadTransactionType(intent)) {
            handleError()
        }
    }

    private fun handleError(throwable: Throwable? = null) {
        throwable?.let { Timber.e(throwable) }
        toast(R.string.transaction_details_error)
        finish()
    }

    private fun loadTransactionType(intent: Intent?): Boolean {
        intent ?: return false
        val txId = intent.getStringExtra(EXTRA_TX_ID) ?: return false
        lifetimeDisposables += viewModel.loadTransactionDetails(txId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                it.transactionId
                displayTransactionDetails(createDetailsFragment(it.safe.asEthereumAddressString(), it.type, it.transaction))
            }, this::handleError)
        return true
    }

    private fun createDetailsFragment(safeAddress: String, type: TransactionType, transaction: Transaction): BaseTransactionDetailsFragment =
        when (type) {
            TransactionType.TOKEN_TRANSFER, TransactionType.ETHER_TRANSFER ->
                ReceiptAssetTransferDetailsFragment.createInstance(transaction, safeAddress)
            TransactionType.REPLACE_SAFE_OWNER, TransactionType.ADD_SAFE_OWNER, TransactionType.REMOVE_SAFE_OWNER ->
                ReceiptChangeSafeSettingsDetailsFragment.createInstance(transaction, safeAddress)
            else -> CreateGenericTransactionDetailsFragment.createInstance(transaction, safeAddress, false)
        }

    override fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this].component)
            .viewModule(ViewModule(this))
            .build().inject(this)
    }

    companion object {

        private const val EXTRA_TX_ID = "extra.string.tx_id"

        fun createIntent(context: Context, transactionId: String) =
            Intent(context, ReceiptTransactionActivity::class.java).apply {
                putExtra(EXTRA_TX_ID, transactionId)
            }
    }

}
