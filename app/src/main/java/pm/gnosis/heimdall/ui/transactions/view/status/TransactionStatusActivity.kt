package pm.gnosis.heimdall.ui.transactions.view.status


import android.content.Context
import android.content.Intent
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_transaction_status.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.transactions.view.TransactionInfoViewHolder
import pm.gnosis.heimdall.utils.DateTimeUtils
import pm.gnosis.heimdall.utils.setupEtherscanTransactionUrl
import pm.gnosis.svalinn.common.utils.appendText
import pm.gnosis.svalinn.common.utils.getColorCompat
import javax.inject.Inject

class TransactionStatusActivity : ViewModelActivity<TransactionStatusContract>() {

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    private var transactionInfoViewHolder: TransactionInfoViewHolder? = null

    override fun screenId() = ScreenId.TRANSACTION_DETAILS

    override fun layout() = R.layout.layout_transaction_status

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onStart() {
        super.onStart()
        val id = intent.getStringExtra(EXTRA_TRANSACTION_ID) ?: ""

        disposables += viewModel.observeUpdates(id)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::applyUpdate)

        disposables += viewModel.observeStatus(id)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::updateStatus)

        disposables += layout_transaction_status_back_button.clicks()
            .subscribeBy { onBackPressed() }

        disposables += toolbarHelper.setupShadow(layout_transaction_status_toolbar_shadow, layout_transaction_status_transaction_info)
    }

    private fun updateStatus(status: TransactionExecutionRepository.PublishStatus) {
        when (status) {
            TransactionExecutionRepository.PublishStatus.Unknown,
            TransactionExecutionRepository.PublishStatus.Pending -> {
                layout_transaction_status_status_value.text = getString(R.string.status_pending)
            }
            is TransactionExecutionRepository.PublishStatus.Failed -> {
                layout_transaction_status_status_value.text =
                        SpannableStringBuilder()
                            .appendText(getString(R.string.status_failed), ForegroundColorSpan(getColorCompat(R.color.tomato)))
                            .append(" - ")
                            .append(DateTimeUtils.getLongTimeString(this, status.timestamp))
            }
            is TransactionExecutionRepository.PublishStatus.Success -> {
                layout_transaction_status_status_value.text =
                        SpannableStringBuilder()
                            .appendText(getString(R.string.status_success), ForegroundColorSpan(getColorCompat(R.color.green)))
                            .append(" - ")
                            .append(DateTimeUtils.getLongTimeString(this, status.timestamp))
            }
        }
    }

    private fun applyUpdate(update: TransactionStatusContract.ViewUpdate) {
        when (update) {
            is TransactionStatusContract.ViewUpdate.Params -> {
                layout_transaction_status_type_value.text = getString(update.type)
                layout_transaction_status_submitted_value.text = DateTimeUtils.getLongTimeString(this, update.submitted)
                layout_transaction_status_fee_value.text = "- ${update.gasToken.displayString(update.gasCosts)}"
                layout_transaction_status_etherscan_link.setupEtherscanTransactionUrl(update.hash, R.string.view_transaction_on)
            }
            is TransactionStatusContract.ViewUpdate.Details ->
                setupViewHolder(update.viewHolder)
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
        layout_transaction_status_info_data_container.removeAllViews()
        // Add view holder views
        viewHolder.inflate(layoutInflater, layout_transaction_status_info_data_container)

        // Register new view holder
        lifecycle.addObserver(viewHolder)
    }

    companion object {
        private const val EXTRA_TRANSACTION_ID = "extra.string.transaction_id"
        fun createIntent(context: Context, id: String) =
            Intent(context, TransactionStatusActivity::class.java).apply {
                putExtra(EXTRA_TRANSACTION_ID, id)
            }
    }
}
