package io.gnosis.safe.ui.assets.coins

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import io.gnosis.safe.databinding.ItemCoinBalanceBinding
import io.gnosis.safe.databinding.ItemCoinTotalBinding
import io.gnosis.safe.ui.base.adapter.Adapter
import io.gnosis.safe.ui.base.adapter.BaseFactory
import io.gnosis.safe.ui.base.adapter.UnsupportedViewType
import io.gnosis.safe.utils.loadTokenLogo

enum class BalanceItemViewType {
    TOTAL,
    COIN
}

class CoinsViewHolderFactory : BaseFactory<BaseCoinsViewHolder<CoinsViewData>, CoinsViewData>() {

    override fun newViewHolder(viewBinding: ViewBinding, viewType: Int): BaseCoinsViewHolder<CoinsViewData> = when (viewType) {
        BalanceItemViewType.TOTAL.ordinal -> TotalBalanceViewHolder(viewBinding as ItemCoinTotalBinding)
        BalanceItemViewType.COIN.ordinal -> CoinBalanceViewHolder(viewBinding as ItemCoinBalanceBinding)
        else -> throw UnsupportedViewType(javaClass.name)
    } as BaseCoinsViewHolder<CoinsViewData>

    override fun layout(layoutInflater: LayoutInflater, parent: ViewGroup, viewType: Int): ViewBinding = when(viewType) {
        BalanceItemViewType.TOTAL.ordinal -> ItemCoinTotalBinding.inflate(layoutInflater, parent, false)
        BalanceItemViewType.COIN.ordinal -> ItemCoinBalanceBinding.inflate(layoutInflater, parent, false)
        else -> throw UnsupportedViewType(javaClass.name)
    }

    override fun viewTypeFor(item: CoinsViewData): Int =
        when (item) {
            is CoinsViewData.TotalBalance -> BalanceItemViewType.TOTAL
            is CoinsViewData.CoinBalance -> BalanceItemViewType.COIN
        }.ordinal
}

abstract class BaseCoinsViewHolder<T : CoinsViewData>(viewBinding: ViewBinding) : Adapter.ViewHolder<T>(viewBinding.root)

class CoinBalanceViewHolder(private val viewBinding: ItemCoinBalanceBinding) : BaseCoinsViewHolder<CoinsViewData.CoinBalance>(viewBinding) {

    override fun bind(coinBalance: CoinsViewData.CoinBalance, payloads: List<Any>) {
        with(viewBinding) {
            logoImage.loadTokenLogo(coinBalance.logoUri)
            symbol.text = coinBalance.symbol
            balance.text = coinBalance.balance
            balanceUsd.text = root.context.getString(coinBalance.balanceFiatStringRes, coinBalance.balanceFiat)
        }
    }
}

class TotalBalanceViewHolder(private val viewBinding: ItemCoinTotalBinding) : BaseCoinsViewHolder<CoinsViewData.TotalBalance>(viewBinding) {

    override fun bind(total: CoinsViewData.TotalBalance, payloads: List<Any>) {
        with(viewBinding) {
            totalBalance.text = root.context.getString(total.balanceFiatStringRes, total.totalFiat)
        }
    }
}
