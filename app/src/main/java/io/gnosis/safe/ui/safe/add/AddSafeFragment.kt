package io.gnosis.safe.ui.safe.add

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.data.models.Safe
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentAddSafeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.helpers.AddressInputHelper
import io.gnosis.safe.toError
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.toColor
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import javax.inject.Inject

class AddSafeFragment : BaseViewBindingFragment<FragmentAddSafeBinding>() {

    private val navArgs by navArgs<AddSafeFragmentArgs>()
    private val selectedChain by lazy { navArgs.chain }

    @Inject
    lateinit var viewModel: AddSafeViewModel

    private val addressInputHelper by lazy {
        AddressInputHelper(
            fragment = this,
            tracker = tracker,
            selectedChain = selectedChain,
            addressCallback = ::updateAddress,
            errorCallback = ::handleError,
            enableUD = viewModel.enableUD(selectedChain),
            enableENS = viewModel.enableENS(selectedChain)
        )
    }

    override fun screenId() = ScreenId.SAFE_ADD_ADDRESS

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddSafeBinding =
        FragmentAddSafeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            nextButton.setOnClickListener {
                addSafeAddressInputLayout.address?.let { viewModel.validate(Safe(it, "", selectedChain.chainId)) }
            }
            backButton.setOnClickListener { findNavController().navigateUp() }
            addSafeAddressInputLayout.setOnClickListener {
                addressInputHelper.showDialog()
            }
            chainRibbon.text = selectedChain.name
            chainRibbon.setTextColor(selectedChain.textColor.toColor(requireContext(), R.color.white))
            chainRibbon.setBackgroundColor(selectedChain.backgroundColor.toColor(requireContext(), R.color.primary))
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (val action = state.viewAction) {
                is ShowValidSafe -> handleValid(action.safe.address)
                is BaseStateViewModel.ViewAction.Loading -> binding.progress.visible(action.isLoading)
                is BaseStateViewModel.ViewAction.ShowError -> handleError(action.error)
            }
        })
    }

    override fun onStop() {
        super.onStop()
        addressInputHelper.hideDialog()
    }

    private fun handleError(throwable: Throwable, input: String? = null) {
        Timber.e(throwable)
        with(binding) {
            progress.visible(false)
            nextButton.isEnabled = false
            bottomLabels.visible(false)

            val error = throwable.toError()
            if (error.trackingRequired) {
                tracker.logException(throwable)
            }
            addSafeAddressInputLayout.setError(error.message(requireContext(), R.string.error_description_safe_address), input)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        addressInputHelper.handleResult(requestCode, resultCode, data)
    }

    private fun updateAddress(address: Solidity.Address) {
        with(binding) {
            nextButton.isEnabled = false
            addSafeAddressInputLayout.setNewAddress(address)
        }
        viewModel.validate(Safe(address, "", selectedChain.chainId))
    }

    private fun handleValid(address: Solidity.Address) {
        with(binding) {
            progress.visible(false)
            binding.nextButton.setOnClickListener {
                findNavController().navigate(
                    AddSafeFragmentDirections.actionAddSafeFragmentToAddSafeNameFragment(address.asEthereumAddressChecksumString(), selectedChain)
                )
            }
            nextButton.isEnabled = true
            bottomLabels.visible(false)
        }
    }
}
