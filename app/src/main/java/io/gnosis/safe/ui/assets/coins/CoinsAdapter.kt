package io.gnosis.safe.ui.assets.coins

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.gnosis.data.models.Chain
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemBannerBinding
import io.gnosis.safe.databinding.ItemCoinBalanceBinding
import io.gnosis.safe.databinding.ItemCoinTotalBinding
import io.gnosis.safe.ui.assets.coins.CoinsViewData.Banner
import io.gnosis.safe.ui.assets.coins.CoinsViewData.TotalBalance
import io.gnosis.safe.ui.assets.coins.CoinsViewData.CoinBalance
import io.gnosis.safe.ui.settings.owner.list.OwnerListFragmentDirections
import io.gnosis.safe.ui.transactions.TransactionsFragmentDirections
import io.gnosis.safe.utils.loadTokenLogo
import timber.log.Timber.i
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
        val bannerIndex = items.indexOfFirst { it  is Banner }
        if (bannerIndex >= 0) {
            items.removeAt(bannerIndex)
            notifyItemRemoved(bannerIndex)
        }
    }

    override fun onBindViewHolder(holder: BaseCoinsViewHolder, position: Int) {
        when (holder) {
            is BannerViewHolder -> {
                val banner = items[position] as Banner
                holder.bind(banner.type, bannerListener)
            }
            is TotalBalanceViewHolder -> {
                val total = items[position] as TotalBalance
                holder.bind(total)
            }
            is CoinBalanceViewHolder -> {
                val balance = items[position] as CoinBalance
                holder.bind(balance)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseCoinsViewHolder {
        return when (BalanceItemViewType.values()[viewType]) {
            BalanceItemViewType.BANNER -> BannerViewHolder(
                ItemBannerBinding.inflate(
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
            is Banner -> BalanceItemViewType.BANNER
            is TotalBalance -> BalanceItemViewType.TOTAL
            is CoinBalance -> BalanceItemViewType.COIN
        }.ordinal
    }

    override fun getItemCount() = items.size

    enum class BalanceItemViewType {
        BANNER,
        TOTAL,
        COIN
    }

    interface OwnerBannerListener {
        fun onBannerDismissed(type: Banner.Type)
        fun onBannerActionTriggered(type: Banner.Type)
    }
}

abstract class BaseCoinsViewHolder(
    viewBinding: ViewBinding
) : RecyclerView.ViewHolder(viewBinding.root)


class CoinBalanceViewHolder(private val viewBinding: ItemCoinBalanceBinding) : BaseCoinsViewHolder(viewBinding) {

    fun bind(coinBalance: CoinBalance) {
        with(viewBinding) {
            logoImage.loadTokenLogo(icon = coinBalance.logoUri)
            symbol.text = coinBalance.symbol
            balance.text = coinBalance.balance
            balanceUsd.text = coinBalance.balanceFiat
            coinContainer.setOnClickListener(View.OnClickListener {
                // TODO Navigate to: Enter recipient address and amount
                i("Send ${symbol.text} funds...")

                //findNavController(this.).navigate(OwnerListFragmentDirections.actionOwnerListFragmentToOwnerAddOptionsFragment())

                navigateToTxDetails(logoImage)
            })
        }

    }
}

private fun navigateToTxDetails(view: View, chain: Chain, id: String) {
    Navigation.findNavController(view).navigate(TransactionsFragmentDirections.actionTransactionsFragmentToTransactionDetailsFragment(chain, id))
}

class TotalBalanceViewHolder(private val viewBinding: ItemCoinTotalBinding) : BaseCoinsViewHolder(viewBinding) {

    fun bind(total: TotalBalance) {
        with(viewBinding) {
            totalBalance.text = total.totalFiat
        }
    }
}

class BannerViewHolder(private val viewBinding: ItemBannerBinding) : BaseCoinsViewHolder(viewBinding) {

    fun bind(type: Banner.Type, bannerListener: WeakReference<CoinsAdapter.OwnerBannerListener>) {
        val context = viewBinding.root.context
        with(viewBinding) {
            when(type) {
                Banner.Type.ADD_OWNER_KEY -> {
                    bannerTitle.text = context.getString(R.string.banner_owner_title)
                    bannerText.text = context.getString(R.string.banner_owner_text)
                    bannerAction.text = context.getString(R.string.banner_owner_action)
                }
                Banner.Type.PASSCODE -> {
                    bannerTitle.text = context.getString(R.string.banner_passcode_title)
                    bannerText.text = context.getString(R.string.banner_passcode_text)
                    bannerAction.text = context.getString(R.string.banner_passcode_action)
                }
            }

            bannerClose.setOnClickListener {
                bannerListener.get()?.onBannerDismissed(type)
            }
            bannerAction.setOnClickListener {
                bannerListener.get()?.onBannerActionTriggered(type)
                when(type) {
                    Banner.Type.ADD_OWNER_KEY -> {
                        Navigation.findNavController(it).navigate(R.id.action_to_add_owner)
                    }
                    Banner.Type.PASSCODE -> {
                        Navigation.findNavController(it).navigate(R.id.action_to_passcode_setup)
                    }
                }
            }
        }
    }
}
