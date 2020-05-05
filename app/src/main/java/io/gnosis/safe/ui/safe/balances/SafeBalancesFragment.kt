package io.gnosis.safe.ui.safe.balances

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.gnosis.data.models.Safe
import io.gnosis.safe.databinding.FragmentSafeBalancesBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.safe.SafeOverviewBaseFragment
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
