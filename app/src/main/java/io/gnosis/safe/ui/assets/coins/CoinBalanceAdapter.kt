package io.gnosis.safe.ui.assets.coins

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.gnosis.data.models.Balance
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemCoinBalanceBinding
import io.gnosis.safe.utils.loadTokenLogo
import io.gnosis.safe.utils.shifted
import java.math.RoundingMode
import java.text.DecimalFormat

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
            logoImage.loadTokenLogo(balanceItem.token.logoUrl)
            //TODO: use balance formatter
            val formatter = DecimalFormat("#0.0#####")
            symbol.text = balanceItem.token.symbol
            balance.text = formatter.format(balanceItem.balance.shifted(balanceItem.token.decimals))
            balanceUsd.text = viewBinding.root.context.getString(
                R.string.usd_balance,
                formatter.format(balanceItem.balanceUsd.setScale(2, RoundingMode.HALF_UP))
            )
        }
    }
}
