package io.gnosis.safe.ui.safe.balances

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import io.gnosis.data.models.Safe
import io.gnosis.safe.R
import io.gnosis.safe.databinding.FragmentSafeBalancesBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.safe.SafeOverviewBaseFragment
import io.gnosis.safe.ui.safe.balances.coins.CoinsFragment
import io.gnosis.safe.ui.safe.balances.collectibles.CollectiblesFragment
import javax.inject.Inject

class SafeBalancesFragment : SafeOverviewBaseFragment<FragmentSafeBalancesBinding>() {

    @Inject
    lateinit var viewModel: SafeBalancesViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSafeBalancesBinding =
        FragmentSafeBalancesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            val adapter = BalancesPagerAdapter(this@SafeBalancesFragment)
            balancesContent.adapter = adapter
            TabLayoutMediator(balancesTabBar, balancesContent, true) { tab, position ->
                if (position % 2 == 0) {
                    tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_coins_24dp)
                    tab.text = getString(R.string.tab_title_coins)
                } else {
                    tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_collectibles_24dp)
                    tab.text = getString(R.string.tab_title_collectibles)
                }
            }.attach()
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is SafeBalancesState.ActiveSafe -> {
                    handleActiveSafe(state.safe)
                    state.viewAction?.let { action ->
                        when (action) {
                            is BaseStateViewModel.ViewAction.NavigateTo -> {
                                findNavController().navigate(action.navDirections)
                            }
                        }
                    }
                }
            }
        })
    }

    override fun handleActiveSafe(safe: Safe?) {
        navHandler?.setSafeData(safe)
    }
}

class BalancesPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private enum class Tabs { COINS, COLLECTIBLES }

    override fun getItemCount(): Int = Tabs.values().size

    override fun createFragment(position: Int): Fragment =
        when (Tabs.values()[position]) {
            Tabs.COINS -> CoinsFragment.newInstance()
            Tabs.COLLECTIBLES -> CollectiblesFragment()

        }
}
