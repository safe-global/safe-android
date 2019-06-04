package pm.gnosis.heimdall.ui.safe.details.transactions

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.layout_adapter_entry_header.view.*
import kotlinx.android.synthetic.main.layout_safe_transactions_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionInfo
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.di.ViewContext
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.ui.base.LifecycleAdapter
import pm.gnosis.heimdall.ui.base.LifecycleAdapter.LifecycleViewHolder
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsContract.AdapterEntry.Header
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsContract.AdapterEntry.Transaction
import pm.gnosis.heimdall.utils.displayString
import pm.gnosis.heimdall.utils.formatAsLongDate
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asDecimalString
import pm.gnosis.utils.removeHexPrefix
import timber.log.Timber
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ForView
class SafeTransactionsAdapter @Inject constructor(
    @ViewContext private val context: Context,
    private val addressHelper: AddressHelper,
    private val viewModel: SafeTransactionsContract
) : LifecycleAdapter<SafeTransactionsContract.AdapterEntry, LifecycleViewHolder<SafeTransactionsContract.AdapterEntry>>(context) {
    val transactionSelectionSubject: PublishSubject<String> = PublishSubject.create()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LifecycleViewHolder<SafeTransactionsContract.AdapterEntry> =
        when (viewType) {
            R.id.adapter_entry_header ->
                HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_adapter_entry_header, parent, false))
            R.id.adapter_entry_transaction ->
                TransactionViewHolder(
                    addressHelper,
                    viewModel,
                    transactionSelectionSubject,
                    LayoutInflater.from(parent.context).inflate(R.layout.layout_safe_transactions_item, parent, false)
                )
            else -> throw IllegalArgumentException("Unknown type")
        }

    override fun getItemViewType(position: Int): Int {
        return items.getOrNull(position)?.type ?: 0
    }

    class HeaderViewHolder(itemView: View) : LifecycleViewHolder<SafeTransactionsContract.AdapterEntry>(itemView) {
        override fun bind(data: SafeTransactionsContract.AdapterEntry, payloads: List<Any>) {
            (data as? Header)?.apply {
                itemView.layout_adapter_entry_header_title.text = title
            }
        }
    }

    class TransactionViewHolder(
        private val addressHelper: AddressHelper,
        private val viewModel: SafeTransactionsContract,
        private val selectionSubject: Subject<String>,
        itemView: View
    ) : LifecycleViewHolder<SafeTransactionsContract.AdapterEntry>(itemView) {

        private val context = itemView.context

        private val disposables = CompositeDisposable()

        private var currentData: Transaction? = null
        private var cachedDetails: TransactionInfo? = null

        override fun bind(data: SafeTransactionsContract.AdapterEntry, payloads: List<Any>) {
            currentData = data as? Transaction
            itemView.layout_safe_transactions_item_timestamp.text = itemView.context.getString(R.string.loading)
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun start() {
            // Make sure no disposable are left over
            disposables.clear()
            val transactionId = currentData?.id ?: return
            itemView.layout_safe_transactions_item_target_label.setTextColor(context.getColorCompat(R.color.dark_slate_blue))
            disposables += viewModel.loadTransactionInfo(transactionId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::updateInfo, {
                    Timber.e(it)
                    itemView.layout_safe_transactions_item_timestamp.text = itemView.context.getString(R.string.transaction_details_error)
                })
            disposables += viewModel.observeTransactionStatus(transactionId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::updateStatus, Timber::e)

            disposables += Observable.interval(60, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ updateTimestamp() }, Timber::e)
        }

        private fun updateStatus(status: TransactionExecutionRepository.PublishStatus) {
            itemView.layout_safe_transactions_item_progress.apply {
                isIndeterminate = false
                max = 100
                progress = 0
            }
            when (status) {
                is TransactionExecutionRepository.PublishStatus.Unknown,
                is TransactionExecutionRepository.PublishStatus.Success -> {
                    itemView.layout_safe_transactions_item_target_image.visible(true)
                    itemView.layout_safe_transactions_item_type_image.visible(false)
                }
                is TransactionExecutionRepository.PublishStatus.Pending -> {
                    itemView.layout_safe_transactions_item_target_image.visible(true)
                    itemView.layout_safe_transactions_item_type_image.visible(false)
                    itemView.layout_safe_transactions_item_progress.isIndeterminate = true
                }
                is TransactionExecutionRepository.PublishStatus.Failed -> {
                    itemView.layout_safe_transactions_item_value.setTextColor(context.getColorCompat(R.color.battleship_grey))
                    itemView.layout_safe_transactions_item_target_label.setTextColor(context.getColorCompat(R.color.tomato))
                    itemView.layout_safe_transactions_item_type_image.visible(true)
                    itemView.layout_safe_transactions_item_type_image.setImageResource(R.drawable.ic_transaction_failed)
                    itemView.layout_safe_transactions_item_target_image.visible(false, View.INVISIBLE)
                }
            }
        }

        private fun updateInfo(info: TransactionInfo) {
            cachedDetails = info
            itemView.setOnClickListener {
                currentData?.let { selectionSubject.onNext(it.id) }
            }
            updateTimestamp()
            when (info.data) {
                is TransactionData.Generic -> {
                    updateViews(
                        address = info.data.to,
                        infoText = context.getString(R.string.x_data_bytes, (info.data.data?.removeHexPrefix()?.length ?: 0) / 2),
                        valueText = "- ${Wei(info.data.value).displayString(context)}",
                        valueColor = R.color.tomato,
                        iconRes = R.drawable.ic_transaction_outgoing
                    )
                }
                is TransactionData.AssetTransfer -> {
                    updateViews(
                        address = info.data.receiver,
                        infoText = null,
                        valueText = null,
                        valueColor = R.color.tomato,
                        iconRes = R.drawable.ic_transaction_outgoing
                    )

                    loadTokenInfo(info.data.token, info.data.amount)
                }
                is TransactionData.ReplaceRecoveryPhrase -> {
                    updateViews(
                        address = info.safe,
                        infoText = context.getString(R.string.replaced_recovery_phrase),
                        valueText = null,
                        valueColor = R.color.dark_slate_blue,
                        iconRes = R.drawable.ic_transaction_white_24dp
                    )
                }
                is TransactionData.ConnectExtension -> {
                    updateViews(
                        address = info.safe,
                        infoText = context.getString(R.string.connected_extension),
                        valueText = null,
                        valueColor = R.color.dark_slate_blue,
                        iconRes = R.drawable.ic_transaction_white_24dp
                    )
                }
            }
        }

        private fun updateViews(address: Solidity.Address, infoText: String?, valueText: String?, valueColor: Int, @DrawableRes iconRes: Int) {
            itemView.layout_safe_transactions_item_type_icon.setImageResource(iconRes)
            itemView.layout_safe_transactions_item_value.text = valueText
            itemView.layout_safe_transactions_item_value.visible(valueText != null)
            itemView.layout_safe_transactions_item_value.setTextColor(context.getColorCompat(valueColor))

            itemView.layout_safe_transactions_item_info.visible(infoText != null)
            itemView.layout_safe_transactions_item_info.text = infoText

            addressHelper.populateAddressInfo(
                itemView.layout_safe_transactions_item_target_label,
                itemView.layout_safe_transactions_item_target_label,
                itemView.layout_safe_transactions_item_target_image,
                address
            ).forEach { disposables += it }
        }

        private fun loadTokenInfo(token: Solidity.Address, amount: BigInteger) {
            disposables += viewModel.loadTokenInfo(token)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        itemView.layout_safe_transactions_item_value.text = "- ${it.displayString(amount)}"
                        itemView.layout_safe_transactions_item_value.visible(true)
                    },
                    onError = {
                        Timber.e(it)
                        itemView.layout_safe_transactions_item_value.text = "- ${amount.asDecimalString()}"
                        itemView.layout_safe_transactions_item_value.visible(true)
                    }
                )
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
