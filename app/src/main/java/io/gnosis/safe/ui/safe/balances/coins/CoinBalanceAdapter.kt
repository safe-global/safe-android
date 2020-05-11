package io.gnosis.safe.ui.safe.balances.coins

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import io.gnosis.data.models.Balance
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemCoinBalanceBinding
import io.gnosis.safe.utils.loadTokenLogo
import io.gnosis.safe.utils.shiftedString

class CoinBalanceAdapter : RecyclerView.Adapter<CoinBalanceViewHolder>() {

    private var balances: List<Balance> = emptyList()

    fun setItems(newCoinBalances: List<Balance>) {
        balances = newCoinBalances
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinBalanceViewHolder =
        CoinBalanceViewHolder(ItemCoinBalanceBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = balances.size

    override fun onBindViewHolder(holder: CoinBalanceViewHolder, position: Int) {
        holder.bind(balances[position])
    }
}

class CoinBalanceViewHolder(private val viewBinding: ItemCoinBalanceBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    fun bind(balanceItem: Balance) {
        with(viewBinding) {
            logoImage.loadTokenLogo(Picasso.get(), balanceItem.token.logoUrl)
            symbol.text = balanceItem.token.symbol
            balance.text = balanceItem.balance.shiftedString(balanceItem.token.decimals, 5)
            balanceUsd.text = viewBinding.root.context.getString(R.string.usd_balance, balanceItem.balanceUsd)
        }
    }
}
