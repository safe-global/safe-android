package pm.gnosis.heimdall.ui.tokens.payment

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.layout_payment_tokens_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20Token.Companion.ETHER_TOKEN
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.di.ViewContext
import javax.inject.Inject

@ForView
class PaymentTokensAdapter @Inject constructor(
    @ViewContext private val context: Context,
    private val viewModel: PaymentTokensContract,
    private val picasso: Picasso
) : ListAdapter<PaymentTokensContract.PaymentToken, PaymentTokensAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.layout_payment_tokens_item, parent, false)
        return ViewHolder((context as LifecycleOwner), viewModel, picasso, itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        onBindViewHolder(holder, position, emptyList())
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.unbind()
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PaymentTokensContract.PaymentToken>() {
            override fun areItemsTheSame(old: PaymentTokensContract.PaymentToken, new: PaymentTokensContract.PaymentToken) =
                old.erc20Token.address == new.erc20Token.address

            override fun areContentsTheSame(old: PaymentTokensContract.PaymentToken, new: PaymentTokensContract.PaymentToken) =
                old == new
        }
    }

    class ViewHolder(
        private val lifecycleOwner: LifecycleOwner,
        private val viewModel: PaymentTokensContract,
        private val picasso: Picasso, itemView: View
    ) :
        LifecycleObserver, RecyclerView.ViewHolder(itemView) {
        private val paymentTokenObserver = Observer<ERC20Token> {
            itemView.payment_tokens_item_radio.isChecked = it.address == current?.erc20Token?.address
        }

        private var current: PaymentTokensContract.PaymentToken? = null

        fun bind(data: PaymentTokensContract.PaymentToken) {
            itemView.payment_tokens_item_symbol.text = data.erc20Token.symbol
            itemView.payment_tokens_item_info.text = data.metricValue

            when {
                data.erc20Token.address == ETHER_TOKEN.address -> itemView.payment_tokens_item_icon.setImageResource(R.drawable.ic_ether_symbol)
                data.erc20Token.logoUrl.isBlank() -> picasso.load(data.erc20Token.logoUrl).into(itemView.payment_tokens_item_icon)
                else -> itemView.payment_tokens_item_icon.setImageDrawable(null)
            }
            current = data

            itemView.payment_tokens_item_radio.isSelected = false
            if (data.selectable) {
                itemView.setOnClickListener {
                    current?.erc20Token?.let { token ->
                        itemView.payment_tokens_item_radio.isSelected = true
                        viewModel.setPaymentToken(token)
                    }
                }
                itemView.setBackgroundResource(R.color.white)
                setAlphaForView(1f)
            } else {
                itemView.setOnClickListener(null)
                itemView.background = null
                setAlphaForView(.5f)
            }
            itemView.isEnabled = data.selectable
            viewModel.paymentToken.observe(lifecycleOwner, paymentTokenObserver)
        }

        private fun setAlphaForView(alpha: Float) {
            itemView.payment_tokens_item_icon.alpha = alpha
            itemView.payment_tokens_item_radio.alpha = alpha
            itemView.payment_tokens_item_symbol.alpha = alpha
            itemView.payment_tokens_item_info.alpha = alpha
        }

        fun unbind() {
            viewModel.paymentToken.removeObserver(paymentTokenObserver)
            current = null
        }
    }
}
