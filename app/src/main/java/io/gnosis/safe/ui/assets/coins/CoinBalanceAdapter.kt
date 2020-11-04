package io.gnosis.safe.ui.assets.coins

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.gnosis.data.models.Balance
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemCoinBalanceBinding
import io.gnosis.safe.databinding.ItemCoinTotalBinding
import io.gnosis.safe.ui.base.adapter.UnsupportedViewType
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.convertAmount
import io.gnosis.safe.utils.loadTokenLogo
import java.math.BigDecimal
import java.math.RoundingMode

class CoinBalanceAdapter(private val balanceFormatter: BalanceFormatter) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var balances: List<Balance> = emptyList()
    private var total: BigDecimal = BigDecimal.ZERO

    fun setItems(coinBalances: List<Balance>, totalBalance: BigDecimal = BigDecimal.ZERO) {
        balances = coinBalances
        total = totalBalance
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        BalanceItemViewType.TOTAL.ordinal -> TotalBalanceViewHolder(ItemCoinTotalBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        BalanceItemViewType.COIN.ordinal -> CoinBalanceViewHolder(ItemCoinBalanceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        else -> throw UnsupportedViewType(javaClass.name)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TotalBalanceViewHolder -> {
                holder.bind(total, balanceFormatter)
            }
            is CoinBalanceViewHolder -> {
                holder.bind(balances[position - 1], balanceFormatter)
            }
        }
    }

    override fun getItemCount(): Int = if (balances.isNotEmpty()) balances.size + 1 else 0

    override fun getItemViewType(position: Int): Int = when {
        position == 0 -> BalanceItemViewType.TOTAL.ordinal
        else -> BalanceItemViewType.COIN.ordinal
    }

    enum class BalanceItemViewType {
        TOTAL,
        COIN
    }

    class CoinBalanceViewHolder(private val viewBinding: ItemCoinBalanceBinding) : RecyclerView.ViewHolder(viewBinding.root) {

        fun bind(balanceItem: Balance, balanceFormatter: BalanceFormatter) {
            with(viewBinding) {
                logoImage.loadTokenLogo(balanceItem.tokenInfo.logoUri)
                symbol.text = balanceItem.tokenInfo.symbol
                balance.text = balanceFormatter.shortAmount(balanceItem.balance.convertAmount(balanceItem.tokenInfo.decimals))
                balanceUsd.text = root.context.getString(
                    R.string.usd_balance,
                    balanceFormatter.shortAmount(balanceItem.fiatBalance.setScale(2, RoundingMode.HALF_UP))
                )
            }
        }
    }

    class TotalBalanceViewHolder(private val viewBinding: ItemCoinTotalBinding) : RecyclerView.ViewHolder(viewBinding.root) {

        fun bind(total: BigDecimal, balanceFormatter: BalanceFormatter) {
            with(viewBinding) {
                totalBalance.text = root.context.getString(
                    R.string.usd_balance,
                    balanceFormatter.shortAmount(total.setScale(2, RoundingMode.HALF_UP))
                )
            }
        }
    }
}


