package io.gnosis.safe.ui.assets.coins

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemBannerImportKeyBinding
import io.gnosis.safe.databinding.ItemCoinBalanceBinding
import io.gnosis.safe.databinding.ItemCoinTotalBinding
import io.gnosis.safe.ui.assets.AssetsFragmentDirections
import io.gnosis.safe.utils.loadTokenLogo
import java.lang.ref.WeakReference

class CoinsAdapter(
    private val bannerListener: WeakReference<OwnerBannerListener>
) : RecyclerView.Adapter<BaseCoinsViewHolder>() {

    private val items = mutableListOf<CoinsViewData>()

    fun updateData(data: List<CoinsViewData>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    fun removeBanner() {
        val bannerIndex = items.indexOf(CoinsViewData.Banner)
        if (bannerIndex >= 0) {
            items.removeAt(bannerIndex)
            notifyItemRemoved(bannerIndex)
        }
    }

    override fun onBindViewHolder(holder: BaseCoinsViewHolder, position: Int) {
        when (holder) {
            is BannerViewHolder -> {
                holder.bind(bannerListener)
            }
            is TotalBalanceViewHolder -> {
                val total = items[position] as CoinsViewData.TotalBalance
                holder.bind(total)
            }
            is CoinBalanceViewHolder -> {
                val balance = items[position] as CoinsViewData.CoinBalance
                holder.bind(balance)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseCoinsViewHolder {
        return when (BalanceItemViewType.values()[viewType]) {
            BalanceItemViewType.BANNER -> BannerViewHolder(
                ItemBannerImportKeyBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            BalanceItemViewType.TOTAL -> TotalBalanceViewHolder(
                ItemCoinTotalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            BalanceItemViewType.COIN -> CoinBalanceViewHolder(
                ItemCoinBalanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return when (item) {
            is CoinsViewData.Banner -> BalanceItemViewType.BANNER
            is CoinsViewData.TotalBalance -> BalanceItemViewType.TOTAL
            is CoinsViewData.CoinBalance -> BalanceItemViewType.COIN
        }.ordinal
    }

    override fun getItemCount() = items.size

    enum class BalanceItemViewType {
        BANNER,
        TOTAL,
        COIN
    }

    interface OwnerBannerListener {
        fun onBannerDismissed()
        fun onBannerActionTriggered()
    }
}

abstract class BaseCoinsViewHolder(
    viewBinding: ViewBinding
) : RecyclerView.ViewHolder(viewBinding.root)


class CoinBalanceViewHolder(private val viewBinding: ItemCoinBalanceBinding) : BaseCoinsViewHolder(viewBinding) {

    fun bind(coinBalance: CoinsViewData.CoinBalance) {
        with(viewBinding) {
            logoImage.loadTokenLogo(icon = coinBalance.logoUri)
            symbol.text = coinBalance.symbol
            balance.text = coinBalance.balance
            balanceUsd.text = coinBalance.balanceFiat
        }
    }
}

class TotalBalanceViewHolder(private val viewBinding: ItemCoinTotalBinding) : BaseCoinsViewHolder(viewBinding) {

    fun bind(total: CoinsViewData.TotalBalance) {
        with(viewBinding) {
            totalBalance.text = total.totalFiat
        }
    }
}

class BannerViewHolder(private val viewBinding: ItemBannerImportKeyBinding) : BaseCoinsViewHolder(viewBinding) {

    fun bind(bannerListener: WeakReference<CoinsAdapter.OwnerBannerListener>) {
        with(viewBinding) {
            bannerClose.setOnClickListener {
                bannerListener.get()?.onBannerDismissed()
            }
            bannerAction.setOnClickListener {
                bannerListener.get()?.onBannerActionTriggered()
                Navigation.findNavController(it).navigate(R.id.action_to_import_owner)
            }
        }
    }
}
