package pm.gnosis.heimdall.ui.transactions.view.review


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.widget.NestedScrollView
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_review_transaction.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.ui.security.unlock.UnlockDialog
import pm.gnosis.heimdall.ui.transactions.view.TransactionInfoViewHolder
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper.Events
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper.ViewUpdate
import pm.gnosis.heimdall.ui.transactions.view.helpers.TransactionSubmitInfoViewHelper
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.toHexString
import timber.log.Timber
import javax.inject.Inject

class ReviewTransactionActivity : ViewModelActivity<ReviewTransactionContract>(), UnlockDialog.UnlockCallback {

    @Inject
    lateinit var infoViewHelper: TransactionSubmitInfoViewHelper

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    private var transactionInfoViewHolder: TransactionInfoViewHolder? = null

    private val unlockStatusSubject = PublishSubject.create<Unit>()

    override fun screenId() = ScreenId.TRANSACTION_REVIEW

    override fun layout() = R.layout.layout_review_transaction

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onUnlockSuccess(requestCode: Int) {
        unlockStatusSubject.onNext(Unit)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val safeAddress = intent.getStringExtra(EXTRA_SAFE_ADDRESS)?.asEthereumAddress() ?: run {
            finish()
            return
        }

        viewModel.setup(safeAddress)
        infoViewHelper.bind(layout_review_transaction_transaction_info)
    }

    override fun onStart() {
        super.onStart()

        val transactionData = intent.extras?.let { TransactionData.fromBundle(it) } ?: run {
            finish()
            return
        }

        infoViewHelper.onToggleReadyState = this::toggleReadyState
        infoViewHelper.toggleRejectionState(false)
        infoViewHelper.resetConfirmationViews()
        infoViewHelper.toggleReadyState(false)

        val submitEvents = unlockStatusSubject
            .doOnNext {
                infoViewHelper.toggleReadyState(false, R.string.submitting_transaction)
            }

        disposables += layout_review_transaction_submit_button.clicks()
            .subscribeBy(onNext = {
                UnlockDialog.create().show(supportFragmentManager, null)
            })

        val events = Events(infoViewHelper.retryEvents(), infoViewHelper.requestConfirmationEvents(), submitEvents)
        disposables += viewModel.observe(events, transactionData)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::applyUpdate, onError = this::dataError)

        disposables += layout_review_transaction_back_button.clicks()
            .subscribeBy { onBackPressed() }

        (layout_review_transaction_transaction_info as? NestedScrollView)?.let {
            disposables += toolbarHelper.setupShadow(layout_review_transaction_toolbar_shadow, it)
        }
    }

    private fun applyUpdate(update: ViewUpdate) {
        when (update) {
            is ViewUpdate.TransactionInfo ->
                setupViewHolder(update.viewHolder)
            is SubmitTransactionHelper.ViewUpdate.TransactionSubmitted -> {
                if (update.success) {
                    startActivity(
                        SafeMainActivity.createIntent(
                            this,
                            null,
                            R.string.tab_title_transactions
                        )
                    )
                } else {
                    infoViewHelper.toggleReadyState(true)
                }
            }
            else ->
                infoViewHelper.applyUpdate(update)?.let { disposables += it }
        }
    }

    private fun toggleReadyState(isReady: Boolean) {
        layout_review_transaction_submit_button.visible(isReady)
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
        errorSnackbar(layout_review_transaction_transaction_info, throwable)
    }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"
        fun createIntent(context: Context, safe: Solidity.Address, txData: TransactionData) =
            Intent(context, ReviewTransactionActivity::class.java).apply {
                putExtra(EXTRA_SAFE_ADDRESS, safe.value.toHexString())
                putExtras(Bundle().apply {
                    txData.addToBundle(this)
                })
            }
    }
}
