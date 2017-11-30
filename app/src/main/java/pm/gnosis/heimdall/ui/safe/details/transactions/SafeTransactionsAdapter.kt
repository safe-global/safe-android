package pm.gnosis.heimdall.ui.safe.details.transactions

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_safe_transactions_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.common.di.ViewContext
import pm.gnosis.heimdall.data.repositories.EtherTransfer
import pm.gnosis.heimdall.data.repositories.TokenTransfer
import pm.gnosis.heimdall.data.repositories.TransactionDetails
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.ui.base.LifecycleAdapter
import pm.gnosis.heimdall.utils.formatAsLongDate
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

@ForView
class SafeTransactionsAdapter @Inject constructor(
        @ViewContext context: Context,
        val transactionDetailRepository: TransactionDetailsRepository
) : LifecycleAdapter<String, SafeTransactionsAdapter.ViewHolder>(context) {
    lateinit var safeAddress: BigInteger
    val tokensSelectionSubject: PublishSubject<String> = PublishSubject.create()

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.layout_safe_transactions_item, parent, false)
        return ViewHolder(view)
    }

    inner class ViewHolder(itemView: View) : LifecycleViewHolder<String>(itemView), View.OnClickListener {

        private val disposables = CompositeDisposable()

        init {
            itemView.setOnClickListener(this)
        }

        private var currentData: String? = null

        override fun bind(data: String, payloads: List<Any>?) {
            currentData = data
            itemView.layout_safe_transactions_item_subject.text = itemView.context.getString(R.string.loading)
            itemView.layout_safe_transactions_item_hash.text = null
            itemView.layout_safe_transactions_item_timestamp.text = null
            itemView.layout_safe_transactions_item_value.visibility = View.GONE
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun start() {
            // Make sure no disposable are left over
            disposables.clear()
            val descriptionHash = currentData ?: return
            disposables += transactionDetailRepository.loadTransactionDetails(descriptionHash, safeAddress, null)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(::updateDetails, Timber::e)

        }

        private fun updateDetails(details: TransactionDetails) {
            itemView.layout_safe_transactions_item_subject.text = details.subject
            itemView.layout_safe_transactions_item_hash.text = details.transactionHash?.subSequence(0, 8)
            itemView.layout_safe_transactions_item_timestamp.text = details.timestamp?.let { itemView.context.formatAsLongDate(it) }
            when (details.type) {
                is EtherTransfer -> {
                    val value = details.type.value.toEther().stripTrailingZeros().toPlainString()
                    val symbol = itemView.context.getString(R.string.currency_eth)
                    itemView.layout_safe_transactions_item_value.text = "$value $symbol"
                    itemView.layout_safe_transactions_item_value.visibility = View.VISIBLE
                }
                is TokenTransfer -> {
                    itemView.layout_safe_transactions_item_value.text = details.type.tokens.toString()
                    itemView.layout_safe_transactions_item_value.visibility = View.VISIBLE
                }
                else -> {
                    itemView.layout_safe_transactions_item_value.visibility = View.GONE
                }
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun stop() {
            disposables.clear()
        }

        override fun unbind() {
            stop()
            currentData = null
            super.unbind()
        }

        override fun onClick(v: View?) {
            tokensSelectionSubject.onNext(items[adapterPosition])
        }
    }
}