package pm.gnosis.heimdall.ui.tokens.payment

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.jakewharton.rxbinding2.widget.checkedChanges
import com.squareup.picasso.Picasso
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_manage_tokens_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.di.ViewContext
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.base.LifecycleAdapter
import timber.log.Timber
import javax.inject.Inject

@ForView
class PaymentTokensAdapter @Inject constructor(
    @ViewContext private val context: Context,
    private val viewModel: PaymentTokensContract,
    private val picasso: Picasso
) : ListAdapter<PaymentTokensContract.PaymentToken, PaymentTokensAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val lifecycle = (context as LifecycleOwner).lifecycle

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(viewModel, picasso, LayoutInflater.from(parent.context).inflate(R.layout.layout_manage_tokens_item, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        super.onBindViewHolder(holder, position, payloads)
        if (payloads.isEmpty()) {
            // If we have a payload this call is a result of notifyItemChanged
            (holder as? LifecycleObserver)?.let { lifecycle.addObserver(it) }
        }
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        onBindViewHolder(holder, position, emptyList())
    }

    override fun onViewRecycled(holder: ViewHolder) {
        (holder as? LifecycleObserver)?.let { lifecycle.removeObserver(it) }
        super.onViewRecycled(holder)
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PaymentTokensContract.PaymentToken>() {
            override fun areItemsTheSame(old: PaymentTokensContract.PaymentToken, new: PaymentTokensContract.PaymentToken) =
                old.erc20Token.address == new.erc20Token.address

            override fun areContentsTheSame(old: PaymentTokensContract.PaymentToken, new: PaymentTokensContract.PaymentToken) =
                old == new
        }
    }

    class ViewHolder(private val viewModel: PaymentTokensContract, private val picasso: Picasso, itemView: View) :
        LifecycleObserver, Adapter.ViewHolder<PaymentTokensContract.PaymentToken>(itemView) {
        private var current: PaymentTokensContract.PaymentToken? = null

        override fun bind(data: PaymentTokensContract.PaymentToken, payloads: List<Any>) {
            itemView.layout_manage_tokens_symbol.text = data.erc20Token.symbol
            itemView.layout_manage_tokens_name.text = data.erc20Token.name
            itemView.layout_manage_tokens_switch.isChecked = data.enabled

            picasso.load(data.erc20Token.logoUrl).into(itemView.layout_manage_tokens_icon)
            current = data
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun start() {

        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun stop() {

        }

        override fun unbind() {
            super.unbind()
            stop()
            current = null
        }
    }
}
