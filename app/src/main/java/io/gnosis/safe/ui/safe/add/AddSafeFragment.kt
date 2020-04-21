package io.gnosis.safe.ui.safe.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.databinding.FragmentAddSafeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment
import kotlinx.coroutines.launch
import javax.inject.Inject

class AddSafeFragment : BaseFragment<FragmentAddSafeBinding>() {

    @Inject
    lateinit var viewModel: AddSafeViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddSafeBinding =
        FragmentAddSafeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            nextButton.setOnClickListener { viewModel.submitAddress(addSafeAddressInputEntry.text.toString()) }
            backButton.setOnClickListener { findNavController().navigateUp() }
        }

        viewModel.state.observe(viewLifecycleOwner, Observer {

        })
    }
}
