package pm.gnosis.heimdall.ui.transactions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.layout_create_transaction.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.transactions.details.assets.CreateAssetTransferDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.base.BaseTransactionDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.generic.CreateGenericTransactionDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.safe.CreateAddOwnerDetailsFragment
import pm.gnosis.heimdall.utils.setupToolbar
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.TransactionParcelable
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.utils.asEthereumAddressString

class CreateTransactionActivity : BaseTransactionActivity() {

    private val transactionDataTransformer: ObservableTransformer<Pair<Solidity.Address?, Result<Transaction>>, Result<Pair<Solidity.Address, Transaction>>> =
        ObservableTransformer { up: Observable<Pair<Solidity.Address?, Result<Transaction>>> ->
            up
                .checkedFlatMap { safeAddress, transaction ->
                    Observable.just<Result<Pair<Solidity.Address, Transaction>>>(DataResult(safeAddress to transaction))
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext({ it.handle(::handleTransactionDataUpdate, ::handleTransactionDataError) })
        }

    override fun screenId() = ScreenId.CREATE_TRANSACTION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_create_transaction)
        setupToolbar(layout_create_transaction_toolbar, R.drawable.ic_close_24dp)
    }

    override fun fragmentRegistered() {
        layout_create_transaction_progress_bar.visibility = View.GONE
    }

    override fun loadTransactionDetails() {
        if (!parseAndDisplayExtras(intent)) {
            toast(R.string.transaction_details_error)
            finish()
        }
    }

    private fun parseAndDisplayExtras(intent: Intent?): Boolean {
        intent ?: return false
        val safe = intent.getStringExtra(EXTRA_SAFE)
        val type = TransactionType.values().getOrNull(intent.getIntExtra(EXTRA_TYPE, -1)) ?: return false
        val transaction = intent.getParcelableExtra<TransactionParcelable>(EXTRA_TRANSACTION)?.transaction
        displayTransactionDetails(createDetailsFragment(safe, type, transaction))
        return true
    }

    private fun createDetailsFragment(safeAddress: String?, type: TransactionType, transaction: Transaction?): BaseTransactionDetailsFragment =
        when (type) {
            TransactionType.TOKEN_TRANSFER, TransactionType.ETHER_TRANSFER ->
                CreateAssetTransferDetailsFragment.createInstance(transaction, safeAddress)
            TransactionType.ADD_SAFE_OWNER ->
                CreateAddOwnerDetailsFragment.createInstance(transaction, safeAddress)
            else -> CreateGenericTransactionDetailsFragment.createInstance(transaction, safeAddress, true)
        }

    private fun handleTransactionDataUpdate(data: Pair<Solidity.Address?, Transaction>) {
        layout_create_transaction_review_button.isEnabled = true
    }

    private fun handleTransactionDataError(error: Throwable) {
        layout_create_transaction_review_button.isEnabled = false
        handleInputError(error)
    }

    override fun transactionDataTransformer(): ObservableTransformer<Pair<Solidity.Address?, Result<Transaction>>, Any> =
        ObservableTransformer {
            it.compose(transactionDataTransformer).switchMap { data ->
                layout_create_transaction_review_button.clicks().map { data }
            }
                .map {
                    when (it) {
                        is DataResult -> startActivity(SubmitTransactionActivity.createIntent(this, it.data.first, it.data.second))
                        is ErrorResult -> handleInputError(it.error)
                    }
                }
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
        private const val EXTRA_TYPE = "extra.int.type"
        private const val EXTRA_TRANSACTION = "extra.parcelable.transaction"

        fun createIntent(context: Context, safeAddress: Solidity.Address?, type: TransactionType, transaction: Transaction?) =
            Intent(context, CreateTransactionActivity::class.java).apply {
                putExtra(EXTRA_SAFE, safeAddress?.asEthereumAddressString())
                putExtra(EXTRA_TYPE, type.ordinal)
                putExtra(EXTRA_TRANSACTION, transaction?.parcelable())
            }
    }
}
