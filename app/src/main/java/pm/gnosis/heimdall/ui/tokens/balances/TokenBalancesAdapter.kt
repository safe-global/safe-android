package pm.gnosis.heimdall.ui.tokens.balances

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.layout_tokens_item_balance.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.common.di.ViewContext
import pm.gnosis.heimdall.data.repositories.models.ERC20Token.Companion.ETHER_TOKEN
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.tokens.info.TokenInfoActivity
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.stringWithNoTrailingZeroes
import javax.inject.Inject


@ForView
class TokenBalancesAdapter @Inject constructor(
        @ViewContext private val context: Context
) : Adapter<ERC20TokenWithBalance, TokenBalancesAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_tokens_item_balance, parent, false)
        return ViewHolder(view)
    }

    inner class ViewHolder(itemView: View) : Adapter.ViewHolder<ERC20TokenWithBalance>(itemView), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
        }

        override fun bind(data: ERC20TokenWithBalance, payloads: List<Any>?) {
            itemView.layout_tokens_item_name.text = if (data.token.name.isNullOrEmpty()) data.token.address.asEthereumAddressString() else data.token.name
            val balance = data.balance?.let { data.token.convertAmount(it).stringWithNoTrailingZeroes() } ?: "-"
            itemView.layout_tokens_item_balance.text = balance
        }

        override fun onClick(v: View?) {
            val token = items[adapterPosition].token
            if (token != ETHER_TOKEN) {
                context.startActivity(TokenInfoActivity.createIntent(context, token.address))
            }
        }
    }
}
