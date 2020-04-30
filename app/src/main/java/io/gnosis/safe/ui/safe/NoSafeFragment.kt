package io.gnosis.safe.ui.safe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation
import io.gnosis.safe.R
import io.gnosis.safe.databinding.FragmentNoSafesBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment

class NoSafeFragment : BaseFragment<FragmentNoSafesBinding>() {

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentNoSafesBinding =
        FragmentNoSafesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.loadSafeButton.setOnClickListener {
            Navigation.findNavController(activity as FragmentActivity, R.id.safe_overview_root).navigate(SafeOverviewFragmentDirections.actionSafeOverviewFragmentToAddSafeNav())
        }
    }
}
