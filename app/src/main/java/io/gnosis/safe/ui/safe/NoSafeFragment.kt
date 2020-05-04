package io.gnosis.safe.ui.safe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.gnosis.data.models.Safe
import io.gnosis.safe.databinding.FragmentNoSafesBinding
import io.gnosis.safe.di.components.ViewComponent

class NoSafeFragment : SafeOverviewBaseFragment<FragmentNoSafesBinding>() {

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentNoSafesBinding =
        FragmentNoSafesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navHandler?.setSafeData(null)
        binding.loadSafeButton.setOnClickListener {
            findNavController().navigate(NoSafeFragmentDirections.actionNoSafeFragmentToAddSafeNav())
        }
    }

    override fun handleActiveSafe(safe: Safe?) {
        TODO("Not yet implemented")
    }
}
