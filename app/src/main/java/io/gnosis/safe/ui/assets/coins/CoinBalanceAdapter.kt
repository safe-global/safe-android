package io.gnosis.safe.ui.assets.coins

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.gnosis.data.models.Balance
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemCoinBalanceBinding
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.convertAmount
import io.gnosis.safe.utils.loadTokenLogo
import java.math.RoundingMode

class CoinBalanceAdapter(private val balanceFormatter: BalanceFormatter) : RecyclerView.Adapter<CoinBalanceViewHolder>() {

    private var balances: List<Balance> = emptyList()

    fun setItems(newCoinBalances: List<Balance>) {
        balances = newCoinBalances
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinBalanceViewHolder =
        CoinBalanceViewHolder(ItemCoinBalanceBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = balances.size

    override fun onBindViewHolder(holder: CoinBalanceViewHolder, position: Int) {
        holder.bind(balances[position], balanceFormatter)
    }
}

class CoinBalanceViewHolder(private val viewBinding: ItemCoinBalanceBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    fun bind(balanceItem: Balance, balanceFormatter: BalanceFormatter) {
        with(viewBinding) {
            logoImage.loadTokenLogo(balanceItem.token.logoUrl)
            symbol.text = balanceItem.token.symbol
            balance.text = balanceFormatter.shortAmount(balanceItem.balance.convertAmount(balanceItem.token.decimals))
            balanceUsd.text = viewBinding.root.context.getString(
                R.string.usd_balance,
                balanceFormatter.shortAmount(balanceItem.balanceUsd.setScale(2, RoundingMode.HALF_UP))
            )
        }
    }
}
