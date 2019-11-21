package pm.gnosis.heimdall.ui.transactions.view.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_multi_send_details.*
import kotlinx.android.synthetic.main.layout_safe_transactions_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionInfoRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.di.ViewContext
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.base.LifecycleAdapter
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.safe.details.transactions.BaseTransactionViewHolder
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.toHexString
import java.math.BigInteger
import javax.inject.Inject

abstract class MultiSendDetailsContract : ViewModel() {
    abstract fun loadTransactionData(tx: SafeTransaction): Single<TransactionData>

    abstract fun loadTokenInfo(token: Solidity.Address): Single<ERC20Token>
}

class MultiSendDetailsViewModel @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val transactionInfoRepository: TransactionInfoRepository
) : MultiSendDetailsContract() {
    override fun loadTransactionData(tx: SafeTransaction): Single<TransactionData> =
        transactionInfoRepository.parseTransactionData(tx)

    override fun loadTokenInfo(token: Solidity.Address) =
        tokenRepository.loadToken(token)

}

class MultiSendDetailsActivity : ViewModelActivity<MultiSendDetailsContract>() {

    @Inject
    lateinit var adapter: MultiSendTransactionsAdapter

    @Inject
    lateinit var layoutManager: LinearLayoutManager

    override fun screenId(): ScreenId? = null

    override fun layout() = R.layout.layout_multi_send_details

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.safe = intent.getStringExtra(EXTRA_SAFE_ADDRESS).asEthereumAddress()!!
        val multiSend = intent.getParcelableExtra<TransactionData.MultiSend>(EXTRA_MULTI_SEND)
        adapter.updateData(Adapter.Data(entries = multiSend.transactions))
        multi_send_details_list.layoutManager = layoutManager
        multi_send_details_list.adapter = adapter
        multi_send_details_back_arrow.setOnClickListener { onBackPressed() }
    }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"
        private const val EXTRA_MULTI_SEND = "extra.parcel.multi_send"
        fun createIntent(context: Context, safe: Solidity.Address, multiSend: TransactionData.MultiSend) =
            Intent(context, MultiSendDetailsActivity::class.java).apply {
                putExtra(EXTRA_SAFE_ADDRESS, safe.value.toHexString())
                putExtra(EXTRA_MULTI_SEND, multiSend)
            }
    }

}

@ForView
class MultiSendTransactionsAdapter @Inject constructor(
    @ViewContext private val context: Context,
    private val addressHelper: AddressHelper,
    private val viewModel: MultiSendDetailsContract
) : LifecycleAdapter<SafeTransaction, LifecycleAdapter.LifecycleViewHolder<SafeTransaction>>(context) {

    lateinit var safe: Solidity.Address

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LifecycleViewHolder<SafeTransaction> =
        TransactionViewHolder(
            safe,
            addressHelper,
            viewModel,
            LayoutInflater.from(parent.context).inflate(R.layout.layout_safe_transactions_item, parent, false)
        )

    class TransactionViewHolder(
        private val safe: Solidity.Address,
        addressHelper: AddressHelper,
        private val viewModel: MultiSendDetailsContract,
        itemView: View
    ) : BaseTransactionViewHolder<SafeTransaction>(addressHelper, itemView) {

        private var currentData: SafeTransaction? = null

        override fun bind(data: SafeTransaction, payloads: List<Any>) {
            currentData = data
            itemView.layout_safe_transactions_item_timestamp.visible(false)
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun start() {
            // Make sure no disposable are left over
            disposables.clear()
            val data = currentData ?: return
            disposables += viewModel.loadTransactionData(data)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { updateData(safe, it) },
                    onError = {
                        updateData(
                            safe,
                            TransactionData.Generic(
                                data.wrapped.address,
                                data.wrapped.value?.value ?: BigInteger.ZERO,
                                data.wrapped.data,
                                data.operation
                            )
                        )
                    }
                )
        }

        override fun tokenInfo(token: Solidity.Address): Single<ERC20Token> =
            viewModel.loadTokenInfo(token)
    }
}
