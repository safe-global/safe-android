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
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.ui.base.LifecycleAdapter
import pm.gnosis.heimdall.utils.formatAsLongDate
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.stringWithNoTrailingZeroes
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

@ForView
class SafeTransactionsAdapter @Inject constructor(
        @ViewContext private val context: Context,
        private val tokenRepository: TokenRepository,
        private val transactionDetailRepository: TransactionDetailsRepository
) : LifecycleAdapter<String, SafeTransactionsAdapter.ViewHolder>(context) {
    lateinit var safeAddress: BigInteger
    val tokensSelectionSubject: PublishSubject<Transaction> = PublishSubject.create()

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.layout_safe_transactions_item, parent, false)
        return ViewHolder(view)
    }

    inner class ViewHolder(itemView: View) : LifecycleViewHolder<String>(itemView) {

        private val disposables = CompositeDisposable()

        private var currentData: String? = null

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
            disposables += transactionDetailRepository.loadTransactionDetails(transactionId, safeAddress, null)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(::updateDetails, Timber::e)

        }

        private fun updateDetails(details: TransactionDetails) {
            itemView.setOnClickListener {
                tokensSelectionSubject.onNext(details.transaction)
            }
            itemView.layout_safe_transactions_item_subject.apply {
                text = details.subject
                visibility = if (details.subject != null) View.VISIBLE else View.GONE
            }
            itemView.layout_safe_transactions_item_to.text = details.transaction.address.asEthereumAddressString()
            itemView.layout_safe_transactions_item_timestamp.text = details.timestamp?.let { itemView.context.formatAsLongDate(it) }
            when (details.type) {
                TransactionType.ETHER_TRANSFER -> {
                    val value = (details.transaction.value ?: Wei.ZERO).toEther().stringWithNoTrailingZeroes()
                    val symbol = itemView.context.getString(R.string.currency_eth)

                    itemView.layout_safe_transactions_item_value.text = context.getString(R.string.outgoing_transaction_value, value, symbol)
                    itemView.layout_safe_transactions_item_value.visibility = View.VISIBLE
                }
                TransactionType.TOKEN_TRANSFER -> {
                    (details.data as? TokenTransferData)?.let {
                        loadTokenValue(details.transaction.address, it.tokens)
                    }
                }
                else -> {
                    itemView.layout_safe_transactions_item_value.visibility = View.GONE
                }
            }
        }

        private fun loadTokenValue(token: BigInteger, value: BigInteger) {
            disposables += tokenRepository.observeToken(token)
                    .map { it.symbol to it.convertAmount(value).setScale(5).stringWithNoTrailingZeroes() }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ (symbol, amount) ->
                        itemView.layout_safe_transactions_item_value.text = context.getString(R.string.outgoing_transaction_value, amount, symbol)
                        itemView.layout_safe_transactions_item_value.visibility = View.VISIBLE
                    }, Timber::e)
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun stop() {
            disposables.clear()
            itemView.setOnClickListener(null)
        }

        override fun unbind() {
            stop()
            currentData = null
            super.unbind()
        }
    }
}