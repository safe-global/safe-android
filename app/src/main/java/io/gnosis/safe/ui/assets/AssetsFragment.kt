package io.gnosis.safe.ui.assets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import io.gnosis.data.models.Safe
import io.gnosis.safe.R
import io.gnosis.safe.databinding.FragmentAssetsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment
import io.gnosis.safe.ui.assets.coins.CoinsFragment
import io.gnosis.safe.ui.assets.collectibles.CollectiblesFragment
import io.gnosis.safe.ui.safe.empty.NoSafeFragment
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class AssetsFragment : SafeOverviewBaseFragment<FragmentAssetsBinding>() {

    @Inject
    lateinit var viewModel: AssetsViewModel

    private lateinit var pager: BalancesPagerAdapter

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAssetsBinding =
        FragmentAssetsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            pager = BalancesPagerAdapter(this@AssetsFragment)
            balancesContent.adapter = pager
            TabLayoutMediator(balancesTabBar, balancesContent, true) { tab, position ->
                when (BalancesPagerAdapter.Tabs.values()[position]) {
                    BalancesPagerAdapter.Tabs.COINS -> {
                        tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_coins_24dp)
                        tab.text = getString(R.string.tab_title_coins)
                    }
                    BalancesPagerAdapter.Tabs.COLLECTIBLES -> {
                        tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_collectibles_24dp)
                        tab.text = getString(R.string.tab_title_collectibles)
                    }
                }
            }.attach()
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is SafeBalancesState.ActiveSafe -> {
                    val noActiveSafe = state.safe == null
                    pager.noActiveSafe = noActiveSafe
                    binding.balancesTabBar.visible(!noActiveSafe, View.INVISIBLE)
                    handleActiveSafe(state.safe)
                }
            }
        })
    }

    override fun handleActiveSafe(safe: Safe?) {
        navHandler?.setSafeData(safe)
    }

    override fun screenId() = null

}

class BalancesPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    enum class Tabs { COINS, COLLECTIBLES }

    enum class Items(val value: Long) {
        COINS(Tabs.COINS.ordinal.toLong()),
        COLLECTIBLES(Tabs.COLLECTIBLES.ordinal.toLong()),
        NO_SAFE(RecyclerView.NO_ID)
    }

    var noActiveSafe: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = if (noActiveSafe) 1 else Tabs.values().size

    override fun createFragment(position: Int): Fragment =
        when (Tabs.values()[position]) {
            Tabs.COINS -> if (noActiveSafe) NoSafeFragment.newInstance(NoSafeFragment.Position.BALANCES) else CoinsFragment.newInstance()
            Tabs.COLLECTIBLES -> CollectiblesFragment.newInstance()
        }

    override fun getItemId(position: Int): Long {
        return when (Tabs.values()[position]) {
            Tabs.COINS -> if (noActiveSafe) Items.NO_SAFE.value else Items.COINS.value
            Tabs.COLLECTIBLES -> Items.COLLECTIBLES.value
        }
    }

    override fun containsItem(itemId: Long): Boolean {
        return when (itemId) {
            Items.NO_SAFE.value -> noActiveSafe
            Items.COINS.value -> !noActiveSafe
            else -> true
        }
    }
}
