package pm.gnosis.heimdall.ui.settings.general.payment

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.picasso.Picasso
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_tokens_item_balance.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20Token.Companion.ETHER_TOKEN
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.di.ViewContext
import pm.gnosis.heimdall.ui.base.Adapter
import javax.inject.Inject

@ForView
class PaymentTokensAdapter @Inject constructor(
    @ViewContext private val context: Context,
    private val picasso: Picasso
) : Adapter<ERC20Token, PaymentTokensAdapter.ViewHolder>() {

    val tokenSelectedSubject = PublishSubject.create<ERC20Token>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_tokens_item_balance, parent, false)
        return ViewHolder(picasso, view)
    }

    inner class ViewHolder(private val picasso: Picasso, itemView: View) : Adapter.ViewHolder<ERC20Token>(itemView), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
        }

        override fun bind(data: ERC20Token, payloads: List<Any>) {
            itemView.layout_tokens_item_balance_symbol.text = data.symbol
            if (data == ETHER_TOKEN) itemView.layout_tokens_item_balance_symbol_image.setImageResource(R.drawable.ic_ether_symbol)
            else picasso.load(data.logoUrl).into(itemView.layout_tokens_item_balance_symbol_image)
        }

        override fun onClick(v: View?) {
            tokenSelectedSubject.onNext(items[adapterPosition])
        }
    }
}
