package io.gnosis.safe.ui.safe.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentAddSafeNameBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.utils.formatEthAddress
import kotlinx.android.synthetic.main.fragment_add_safe.*
import pm.gnosis.svalinn.common.utils.hideSoftKeyboard
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import timber.log.Timber
import javax.inject.Inject

class AddSafeNameFragment : BaseViewBindingFragment<FragmentAddSafeNameBinding>() {

    @Inject
    lateinit var viewModel: AddSafeNameViewModel

    private val navArgs by navArgs<AddSafeNameFragmentArgs>()
    private val newAddress by lazy { navArgs.newAddress.asEthereumAddress()!! }

    override fun screenId() = ScreenId.SAFE_ADD_NAME

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddSafeNameBinding =
        FragmentAddSafeNameBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            newAddressBlockies.setAddress(newAddress)
            newAddressHex.text = newAddress.formatEthAddress(requireContext(), addMiddleLinebreak = false)
            backButton.setOnClickListener { findNavController().navigateUp() }
            nextButton.setOnClickListener {
                addSafeNameLayout.isErrorEnabled = false
                viewModel.submitAddressAndName(newAddress, addSafeNameEntry.text.toString())
            }
            addSafeNameEntry.doOnTextChanged { text, _, _, _ -> binding.nextButton.isEnabled = !text.isNullOrBlank() }
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is AddSafeNameState -> {
                    state.viewAction?.let { action ->
                        when (action) {
                            is BaseStateViewModel.ViewAction.CloseScreen -> {
                                requireActivity().hideSoftKeyboard()
                                findNavController().popBackStack(R.id.addSafeFragment, true)
                            }
                            is BaseStateViewModel.ViewAction.Loading -> binding.progress.visible(action.isLoading)
                            is BaseStateViewModel.ViewAction.ShowError -> {
                                progress.visible(false)
                                binding.addSafeNameLayout.isErrorEnabled = true
                                binding.addSafeNameLayout.error = getString(R.string.error_invalid_name)
                                binding.nextButton.isEnabled = false
                                Timber.e(action.error)
                            }
                            else -> Timber.i("Unsupported action by view: $action")
                        }
                    }
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().hideSoftKeyboard()
    }
}
