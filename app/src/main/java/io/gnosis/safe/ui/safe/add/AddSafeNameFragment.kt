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
import io.gnosis.safe.toError
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.*
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.formatEthAddress
import io.gnosis.safe.utils.toColor
import pm.gnosis.svalinn.common.utils.hideSoftKeyboard
import pm.gnosis.utils.asEthereumAddress
import timber.log.Timber
import javax.inject.Inject

class AddSafeNameFragment : BaseViewBindingFragment<FragmentAddSafeNameBinding>() {

    @Inject
    lateinit var viewModel: AddSafeNameViewModel

    private val navArgs by navArgs<AddSafeNameFragmentArgs>()
    private val newAddress by lazy { navArgs.newAddress.asEthereumAddress()!! }
    private val selectedChain by lazy { navArgs.chain }

    override fun screenId() = ScreenId.SAFE_ADD_NAME

    override suspend fun chainId() = selectedChain.chainId

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
            val chainPrefix = if (viewModel.isChainPrefixPrependEnabled()) selectedChain.shortName else null
            newAddressHex.text = newAddress.formatEthAddress(requireContext(), chainPrefix, addMiddleLinebreak = false)
            backButton.setOnClickListener { findNavController().navigateUp() }
            nextButton.setOnClickListener {
                addSafeNameLayout.isErrorEnabled = false
                viewModel.submitAddressAndName(newAddress, addSafeNameEntry.text.toString(), selectedChain)
            }
            addSafeNameEntry.doOnTextChanged { text, _, _, _ -> binding.nextButton.isEnabled = !text.isNullOrBlank() }

            chainRibbon.text = selectedChain.name
            chainRibbon.setTextColor(selectedChain.textColor.toColor(requireContext(), R.color.white))
            chainRibbon.setBackgroundColor(selectedChain.backgroundColor.toColor(requireContext(), R.color.primary))
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is AddSafeNameState -> {
                    state.viewAction?.let { action ->
                        when (action) {
                            is CloseScreen -> {
                                requireActivity().hideSoftKeyboard()
                                findNavController().popBackStack(R.id.selectChainFragment, true)
                            }
                            is ImportOwner -> {
                                requireActivity().hideSoftKeyboard()
                                findNavController().navigate(AddSafeNameFragmentDirections.actionAddSafeNameFragmentToAddSafeOwnerFragment(binding.addSafeNameEntry.text.toString(), navArgs.newAddress, selectedChain))
                            }
                            is Loading -> {}
                            is ShowError -> {
                                val error = action.error.toError()
                                binding.addSafeNameLayout.error = error.message(requireContext(), R.string.error_description_safe_add)
                                binding.addSafeNameLayout.isErrorEnabled = true
                                binding.nextButton.isEnabled = false
                                Timber.e(action.error)
                            }
                            else -> Timber.e("Unsupported action by view: $action")
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
