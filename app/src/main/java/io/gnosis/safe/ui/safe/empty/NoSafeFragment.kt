package io.gnosis.safe.ui.safe.empty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.gnosis.data.models.Safe
import io.gnosis.safe.databinding.FragmentNoSafesBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.safe.SafeOverviewBaseFragment
import javax.inject.Inject

class NoSafeFragment : SafeOverviewBaseFragment<FragmentNoSafesBinding>() {

    @Inject
    lateinit var viewModel: NoSafeViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentNoSafesBinding =
        FragmentNoSafesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loadSafeButton.setOnClickListener {
            findNavController().navigate(NoSafeFragmentDirections.actionNoSafeFragmentToAddSafeNav())
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            handleActiveSafe(state.activeSafe)
            state.viewAction?.let { action ->
                when(action) {
                    is BaseStateViewModel.ViewAction.NavigateTo -> {
                        findNavController().navigate(action.navDirections)
                    }
                }
            }
        })
    }

    override fun handleActiveSafe(safe: Safe?) {
        navHandler?.setSafeData(null)
    }
}
