package pm.gnosis.heimdall.ui.transactions.review


import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.content.ContextCompat
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_review_transaction.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.ui.security.unlock.UnlockDialog
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.stringWithNoTrailingZeroes
import pm.gnosis.utils.toHexString
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ReviewTransactionActivity : ViewModelActivity<ReviewTransactionContract>(), UnlockDialog.UnlockCallback {

    private var transactionInfoViewHolder: TransactionInfoViewHolder? = null

    private val unlockStatusSubject = PublishSubject.create<Unit>()

    override fun screenId() = ScreenId.REVIEW_TRANSACTION

    override fun layout() = R.layout.layout_review_transaction

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onUnlockSuccess() {
        unlockStatusSubject.onNext(Unit)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val safeAddress = intent.getStringExtra(EXTRA_SAFE_ADDRESS)?.asEthereumAddress() ?: run {
            finish()
            return
        }

        viewModel.setup(safeAddress)
    }

    override fun onStart() {
        super.onStart()

        val transactionData = intent.extras?.let { TransactionData.fromBundle(it) } ?: run {
            finish()
            return
        }

        toggleRejectionState(false)
        toggleReadyState(false)
        layout_review_transaction_confirmations_group.visible(false)
        val retryEvents = layout_review_transaction_retry_button.clicks()
            .doOnNext {
                layout_review_transaction_confirmation_progress.visible(true)
                layout_review_transaction_retry_button.visible(false)
            }

        layout_review_transaction_request_button.isEnabled = false
        layout_review_transaction_confirmations_timer.text = getString(R.string.request_confirmation_wait_x_s, 30.toString())
        val requestConfirmationEvents = layout_review_transaction_request_button.clicks()
            .doOnNext {
                toggleRejectionState(false)
                layout_review_transaction_request_button.isEnabled = false
                layout_review_transaction_confirmations_timer.text = getString(R.string.request_confirmation_wait_x_s, 30.toString())
            }

        val submitEvents = unlockStatusSubject
            .doOnNext {
                toggleReadyState(false, R.string.submitting_transaction)
            }

        disposables += layout_review_transaction_submit_button.clicks()
            .subscribeBy(onNext = {
                UnlockDialog().show(supportFragmentManager, null)
            })

        val events = ReviewTransactionContract.Events(retryEvents, requestConfirmationEvents, submitEvents)
        disposables += viewModel.observe(events, transactionData)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::applyUpdate, onError = this::dataError)

        disposables += layout_review_transaction_back_button.clicks()
            .subscribeBy { onBackPressed() }
    }

    private fun applyUpdate(update: ReviewTransactionContract.ViewUpdate) {
        when (update) {
            is ReviewTransactionContract.ViewUpdate.TransactionInfo ->
                setupViewHolder(update.viewHolder)
            is ReviewTransactionContract.ViewUpdate.Estimate -> {
                layout_review_transaction_data_balance_value.text = getString(R.string.x_ether, update.balance.toEther().stringWithNoTrailingZeroes())
                layout_review_transaction_data_fees_value.text =
                        "- ${getString(R.string.x_ether, update.fees.toEther().stringWithNoTrailingZeroes())}"
                layout_review_transaction_confirmations_group.visible(true)
            }
            is ReviewTransactionContract.ViewUpdate.EstimateError -> {
                layout_review_transaction_confirmation_progress.visible(false)
                layout_review_transaction_retry_button.visible(true)
            }
            is ReviewTransactionContract.ViewUpdate.Confirmations -> {
                toggleReadyState(update.isReady)
                layout_review_transaction_confirmations_group.visible(!update.isReady)
            }
            is ReviewTransactionContract.ViewUpdate.ConfirmationsRequested -> {
                disposables += Observable.interval(1, TimeUnit.SECONDS).take(30)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onNext = {
                        layout_review_transaction_confirmations_timer.text =
                                getString(R.string.request_confirmation_wait_x_s, (30 - it - 1).toString().padStart(2, '0'))
                    }, onComplete = {
                        layout_review_transaction_request_button.isEnabled = true
                        layout_review_transaction_confirmations_timer.text = null
                    })
            }
            is ReviewTransactionContract.ViewUpdate.ConfirmationsError -> {
                layout_review_transaction_request_button.isEnabled = true
                layout_review_transaction_confirmations_timer.text = null
            }
            ReviewTransactionContract.ViewUpdate.TransactionRejected -> {
                toggleRejectionState(true)
            }
            is ReviewTransactionContract.ViewUpdate.TransactionSubmitted -> {
                if (update.success) {
                    startActivity(
                        SafeMainActivity.createIntent(
                            this,
                            null,
                            R.string.tab_title_transactions
                        )
                    )
                } else {
                    toggleReadyState(true)
                }
            }
        }
    }

    private fun toggleRejectionState(rejected: Boolean) {
        layout_review_transaction_confirmation_progress.apply {
            isIndeterminate = !rejected
            max = 100
            progress = 100
        }
        layout_review_transaction_confirmation_progress.progressTintList =
                ColorStateList.valueOf(getColorCompat( if (rejected) R.color.tomato_15 else R.color.azure ))
        layout_review_transaction_confirmation_progress.progressTintMode = PorterDuff.Mode.SRC_IN
        layout_review_transaction_confirmations_image.setImageResource(
            if (rejected) R.drawable.ic_rejected_extension else R.drawable.ic_confirmations_waiting
        )
        layout_review_transaction_confirmations_info.setTextColor(
            getColorCompat(if (rejected) R.color.tomato else R.color.dark_slate_blue)
        )
        layout_review_transaction_confirmations_info.text =
                getString(if (rejected) R.string.rejected_by_extension else R.string.confirm_with_extension)
    }

    private fun toggleReadyState(isReady: Boolean, inProgressMessage: Int = R.string.awaiting_confirmations) {
        layout_review_transaction_confirmation_status.text = getString(
            if (isReady) R.string.confirmations_ready
            else inProgressMessage
        )
        layout_review_transaction_submit_button.visible(isReady)
        layout_review_transaction_confirmation_progress.apply {
            isIndeterminate = !isReady
            max = 100
            progress = 100
        }

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

        // Remove previous views (normally the progress bar)
        layout_review_transaction_data_container.removeAllViews()
        // Add view holder views
        viewHolder.inflate(layoutInflater, layout_review_transaction_data_container)

        // Register new view holder
        lifecycle.addObserver(viewHolder)
    }

    private fun dataError(throwable: Throwable) {
        Timber.e(throwable)
        errorSnackbar(layout_review_transaction_data_container, throwable)
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
