package pm.gnosis.heimdall.ui.transactions

import android.content.Intent
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v7.widget.Toolbar
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.setupToolbar
import pm.gnosis.heimdall.common.utils.toast
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.ui.transactions.details.assets.ReviewAssetTransferDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.base.BaseTransactionDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.generic.CreateGenericTransactionDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.safe.ReviewChangeDeviceSettingsDetailsFragment
import pm.gnosis.models.Transaction
import pm.gnosis.models.TransactionParcelable
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject


abstract class ViewTransactionActivity : BaseTransactionActivity() {

    @Inject
    lateinit var viewModel: ViewTransactionContract

    // Used for disposables that exists from onCreate to onDestroy
    private val lifetimeDisposables = CompositeDisposable()

    @LayoutRes
    abstract fun layout(): Int

    abstract fun toolbar(): Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout())

        setupToolbar(toolbar(), R.drawable.ic_close_24dp)
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
                TransactionType.REPLACE_SAFE_OWNER, TransactionType.ADD_SAFE_OWNER, TransactionType.REMOVE_SAFE_OWNER ->
                    ReviewChangeDeviceSettingsDetailsFragment.createInstance(transaction, safeAddress)
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

    companion object {

        private const val EXTRA_SAFE = "extra.string.safe"
        private const val EXTRA_TRANSACTION = "extra.parcelable.transaction"

        fun createBundle(safeAddress: BigInteger?, transaction: Transaction) =
                Bundle().apply {
                    putString(EXTRA_SAFE, safeAddress?.asEthereumAddressString())
                    putParcelable(EXTRA_TRANSACTION, transaction.parcelable())
                }
    }
}