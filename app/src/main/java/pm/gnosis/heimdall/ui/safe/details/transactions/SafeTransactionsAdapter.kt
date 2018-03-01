package pm.gnosis.heimdall.ui.safe.details.transactions

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_safe_transactions_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TransactionDetails
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.ui.base.LifecycleAdapter
import pm.gnosis.heimdall.utils.formatAsLongDate
import pm.gnosis.svalinn.common.di.ForView
import pm.gnosis.svalinn.common.di.ViewContext
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ForView
class SafeTransactionsAdapter @Inject constructor(
    @ViewContext private val context: Context,
    private val viewModel: SafeTransactionsContract
) : LifecycleAdapter<String, SafeTransactionsAdapter.ViewHolder>(context) {
    val transactionSelectionSubject: PublishSubject<String> = PublishSubject.create()

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.layout_safe_transactions_item, parent, false)
        return ViewHolder(view)
    }

    inner class ViewHolder(itemView: View) : LifecycleViewHolder<String>(itemView) {

        private val disposables = CompositeDisposable()

        private var currentData: String? = null
        private var cachedDetails: TransactionDetails? = null

        override fun bind(data: String, payloads: List<Any>?) {
            currentData = data
            itemView.layout_safe_transactions_item_timestamp.text = itemView.context.getString(R.string.loading)
            itemView.layout_safe_transactions_item_value.visibility = View.GONE
            itemView.layout_safe_transactions_item_subject.visibility = View.GONE
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun start() {
            // Make sure no disposable are left over
            disposables.clear()
            val transactionId = currentData ?: return
            disposables += viewModel.loadTransactionDetails(transactionId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ (details, transferInfo) -> updateDetails(details, transferInfo) }, {
                    Timber.e(it)
                    itemView.layout_safe_transactions_item_timestamp.text = itemView.context.getString(R.string.transaction_details_error)
                })
            disposables += viewModel.observeTransactionStatus(transactionId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::updateStatus, Timber::e)
        }

        private fun updateStatus(status: TransactionRepository.PublishStatus) {
            itemView.layout_safe_transactions_item_status.apply {
                when (status) {
                    TransactionRepository.PublishStatus.UNKNOWN, TransactionRepository.PublishStatus.SUCCESS -> {
                        visibility = View.GONE
                    }
                    TransactionRepository.PublishStatus.PENDING -> {
                        setTextColor(ContextCompat.getColor(context, R.color.light_text))
                        text = context.getString(R.string.status_pending)
                        visibility = View.VISIBLE
                    }
                    TransactionRepository.PublishStatus.FAILED -> {
                        setTextColor(ContextCompat.getColor(context, R.color.error))
                        text = context.getString(R.string.status_failed)
                        visibility = View.VISIBLE
                    }
                }
            }
        }

        private fun updateDetails(details: TransactionDetails, transferInfo: SafeTransactionsContract.TransferInfo?) {
            cachedDetails = details
            itemView.setOnClickListener {
                currentData?.let { transactionSelectionSubject.onNext(it) }
            }
            itemView.layout_safe_transactions_item_subject.apply {
                text = details.subject
                visibility = if (details.subject != null) View.VISIBLE else View.GONE
            }
            itemView.layout_safe_transactions_item_to.text = details.transaction.address.asEthereumAddressString()
            if (transferInfo != null) {
                itemView.layout_safe_transactions_item_value.text =
                        context.getString(R.string.outgoing_transaction_value, transferInfo.amount, transferInfo.symbol)
                itemView.layout_safe_transactions_item_value.visibility = View.VISIBLE
            } else {
                itemView.layout_safe_transactions_item_value.visibility = View.GONE
            }
            updateTimestamp()
            disposables += Observable.interval(60, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ updateTimestamp() }, Timber::e)
        }

        private fun updateTimestamp() {
            cachedDetails?.timestamp?.let {
                itemView.layout_safe_transactions_item_timestamp.text = itemView.context.formatAsLongDate(it)
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun stop() {
            disposables.clear()
            cachedDetails = null
            itemView.setOnClickListener(null)
        }

        override fun unbind() {
            stop()
            currentData = null
            super.unbind()
        }
    }
}
