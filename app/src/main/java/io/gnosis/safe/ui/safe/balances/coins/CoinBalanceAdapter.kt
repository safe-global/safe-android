package io.gnosis.safe.ui.safe.balances.coins

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import io.gnosis.data.models.Balance
import io.gnosis.safe.databinding.ItemCoinBalanceBinding
import io.gnosis.safe.utils.loadTokenLogo

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

    fun bind(balance: Balance) {
        viewBinding.logoImage.loadTokenLogo(Picasso.get(), balance.token.logoUrl)
    }
}
