package pm.gnosis.heimdall.ui.tokens.overview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_tokens_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.nullOnThrow
import pm.gnosis.utils.stringWithNoTrailingZeroes
import pm.gnosis.utils.withTokenScaleOrNull
import java.math.BigDecimal
import javax.inject.Inject


@ForView
class TokensAdapter @Inject constructor() : Adapter<TokensContract.ERC20TokenWithBalance, TokensAdapter.ViewHolder>() {
    val tokensSelectionSubject: PublishSubject<ERC20Token> = PublishSubject.create()
    val tokenRemovalSubject: PublishSubject<ERC20Token> = PublishSubject.create()
    var itemsClickable = true

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.layout_tokens_item, parent, false)
        return ViewHolder(view)
    }

    inner class ViewHolder(itemView: View) : Adapter.ViewHolder<TokensContract.ERC20TokenWithBalance>(itemView), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
            itemView.layout_tokens_item_delete.setOnClickListener {
                tokenRemovalSubject.onNext(items[adapterPosition].token)
            }
        }

        override fun bind(data: TokensContract.ERC20TokenWithBalance) {
            itemView.layout_tokens_item_name.text = if (data.token.name.isNullOrEmpty()) data.token.address.asEthereumAddressString() else data.token.name
            itemView.layout_tokens_item_delete.visibility = if (data.token.verified == true) View.GONE else View.VISIBLE
            val balance = nullOnThrow { BigDecimal(data.balance).withTokenScaleOrNull(data.token.decimals)?.stringWithNoTrailingZeroes() } ?: "-"
            itemView.layout_tokens_item_balance.text = balance
        }

        override fun onClick(v: View?) {
            if (itemsClickable) {
                tokensSelectionSubject.onNext(items[adapterPosition].token)
            }
        }
    }
}
