package io.gnosis.safe.ui.safe.balances

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import io.gnosis.data.models.Safe
import io.gnosis.safe.R
import io.gnosis.safe.databinding.FragmentSafeBalancesBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.safe.SafeOverviewBaseFragment
import io.gnosis.safe.ui.safe.balances.coins.CoinsFragment
import io.gnosis.safe.ui.safe.balances.collectibles.CollectiblesFragment
import io.gnosis.safe.ui.safe.empty.NoSafeFragment
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class SafeBalancesFragment : SafeOverviewBaseFragment<FragmentSafeBalancesBinding>() {

    @Inject
    lateinit var viewModel: SafeBalancesViewModel

    private lateinit var pager: BalancesPagerAdapter

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSafeBalancesBinding =
        FragmentSafeBalancesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            pager = BalancesPagerAdapter(this@SafeBalancesFragment)
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
                    pager.noActiveSafe = false
                    binding.balancesTabBar.visible(true, View.INVISIBLE)
                    handleActiveSafe(state.safe)
                }
                is SafeBalancesState.NoActiveSafe -> {
                    pager.noActiveSafe = true
                    binding.balancesTabBar.visible(false, View.INVISIBLE)
                    handleActiveSafe(null)
                }
            }
        })
    }

    override fun handleActiveSafe(safe: Safe?) {
        navHandler?.setSafeData(safe)
    }
}

class BalancesPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    enum class Tabs { COINS, COLLECTIBLES }

    var noActiveSafe: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = if (noActiveSafe) 1 else Tabs.values().size

    override fun createFragment(position: Int): Fragment =
        when (Tabs.values()[position]) {
            Tabs.COINS -> if (noActiveSafe) NoSafeFragment.newInstance() else CoinsFragment.newInstance()
            Tabs.COLLECTIBLES -> CollectiblesFragment.newInstance()
        }

    // item ids:
    // COINS -> 1
    // COLLECTIBLES -> 2
    // NO_SAFE -> 3
    override fun getItemId(position: Int): Long {
        return when (Tabs.values()[position]) {
            Tabs.COINS -> if (noActiveSafe) 3L else 1L
            Tabs.COLLECTIBLES -> 2L
        }
    }

    override fun containsItem(itemId: Long): Boolean {
        return when {
            noActiveSafe && itemId == 3L -> true
            noActiveSafe && itemId != 3L -> false
            else -> itemId == 1L || itemId == 2L
        }
    }
}
