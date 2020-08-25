package io.gnosis.safe.ui.safe.add

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentAddSafeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.helpers.AddressInputHelper
import io.gnosis.safe.helpers.Offline
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.exceptions.InvalidAddressException
import timber.log.Timber
import javax.inject.Inject

class AddSafeFragment : BaseViewBindingFragment<FragmentAddSafeBinding>() {

    @Inject
    lateinit var viewModel: AddSafeViewModel

    private val addressInputHelper by lazy {
        AddressInputHelper(this, tracker, ::updateAddress, allowAddressBook = false, errorCallback = ::handleError)
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
                addSafeAddressInputLayout.address?.let { viewModel.validate(it) }
            }
            backButton.setOnClickListener { findNavController().navigateUp() }
            addSafeAddressInputLayout.setOnClickListener {
                addressInputHelper.showDialog()
            }
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (val action = state.viewAction) {
                is ShowValidSafe -> handleValid(action.address)
                is BaseStateViewModel.ViewAction.Loading -> binding.progress.visible(action.isLoading)
                is BaseStateViewModel.ViewAction.ShowError -> handleError(action.error)
            }
        })
    }

    private fun handleError(throwable: Throwable) {
        Timber.e(throwable)
        with(binding) {
            progress.visible(false)
            nextButton.isEnabled = false
            when(throwable) {
                is InvalidAddressException -> {
                    addSafeAddressInputLayout.address = null
                    snackbar(requireView(), R.string.invalid_ethereum_address, Snackbar.LENGTH_LONG)
                }
                is Offline -> {
                    addSafeAddressInputLayout.address = null
                    snackbar(requireView(), R.string.error_no_internet)
                }
                else -> {
                    addSafeAddressInputLayout.error =
                        when (throwable) {
                            is UsedSafeAddress -> getString(R.string.error_used_address)
                            else -> getString(R.string.error_invalid_safe)
                        }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        addressInputHelper.handleResult(requestCode, resultCode, data)
    }

    private fun updateAddress(address: Solidity.Address) {
        viewModel.validate(address)
    }

    private fun handleValid(address: Solidity.Address) {
        with(binding) {
            progress.visible(false)
            binding.nextButton.setOnClickListener {
                findNavController().navigate(
                    AddSafeFragmentDirections.actionAddSafeFragmentToAddSafeNameFragment(address.asEthereumAddressChecksumString())
                )
            }
            nextButton.isEnabled = true
            addSafeAddressInputLayout.address = address
            addSafeAddressInputLayout.error = null
            bottomLabels.visible(false)
        }
    }
}
