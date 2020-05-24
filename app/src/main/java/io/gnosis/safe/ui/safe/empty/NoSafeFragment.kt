package io.gnosis.safe.ui.safe.empty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.R
import io.gnosis.safe.databinding.FragmentNoSafesBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment

class NoSafeFragment : BaseFragment<FragmentNoSafesBinding>() {

    override fun inject(component: ViewComponent) {}

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentNoSafesBinding =
        FragmentNoSafesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loadSafeButton.setOnClickListener {
            requireParentFragment().findNavController().navigate(R.id.action_to_add_safe_nav)
        }
    }

    companion object {

        fun newInstance(): NoSafeFragment {
           return NoSafeFragment()
        }
    }
}
