package io.gnosis.safe.ui.safe.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.gnosis.data.models.Safe
import io.gnosis.safe.databinding.FragmentSafeSettingsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.safe.SafeOverviewBaseFragment
import pm.gnosis.svalinn.common.utils.snackbar
import javax.inject.Inject

class SafeSettingsFragment : SafeOverviewBaseFragment<FragmentSafeSettingsBinding>() {

    @Inject
    lateinit var viewModel: SafeSettingsViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSafeSettingsBinding =
        FragmentSafeSettingsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.button.setOnClickListener {
            viewModel.removeSafe()
        }
        viewModel.state.observe(viewLifecycleOwner, Observer { state ->

            when(state) {
                is SafeSettingsState.SafeLoading -> {
                    when(state.viewAction) {
                        is BaseStateViewModel.ViewAction.ShowError -> {
                            snackbar(binding.root, "safe not loaded!")
                        }
                    }

                }
                is SafeSettingsState.SafeSettings -> {
                    handleActiveSafe(state.safe)
                    if (state.safe == null) {
                        findNavController().popBackStack()
                    } else {
                        binding.name.text = state.safe?.localName
                    }
                }
                is SafeSettingsState.SafeRemoved -> {
                    state.viewAction?.let { action ->
                        when (action) {
                            is BaseStateViewModel.ViewAction.NavigateTo -> findNavController().navigate(action.navDirections)
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



