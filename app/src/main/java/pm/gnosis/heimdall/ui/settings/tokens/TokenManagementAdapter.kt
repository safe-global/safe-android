package pm.gnosis.heimdall.ui.settings.tokens

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.layout_tokens_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.di.ViewContext
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.tokens.info.TokenInfoActivity
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject

@ForView
class TokenManagementAdapter @Inject constructor(
    @ViewContext private val context: Context
) : Adapter<ERC20Token, TokenManagementAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_tokens_item, parent, false))


    inner class ViewHolder(itemView: View) : Adapter.ViewHolder<ERC20Token>(itemView) {
        init {
            itemView.setOnClickListener {
                val token = items[adapterPosition]
                context.startActivity(TokenInfoActivity.createIntent(context, token.address))
            }
        }

        override fun bind(data: ERC20Token, payloads: List<Any>) {
            itemView.layout_tokens_item_name.text = data.name ?: data.address.asEthereumAddressString()
            itemView.layout_tokens_item_symbol.text =
                    if (data.symbol.isNullOrEmpty()) data.address.asEthereumAddressString().substring(0, 3)
                    else data.symbol
        }
    }
}
