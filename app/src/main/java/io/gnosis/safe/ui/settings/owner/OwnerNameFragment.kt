package io.gnosis.safe.ui.settings.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentOwnerNameBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.formatEthAddress
import kotlinx.coroutines.launch
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexAsBigInteger
import javax.inject.Inject

class OwnerNameFragment : BaseViewBindingFragment<FragmentOwnerNameBinding>() {

    @Inject
    lateinit var viewModel: OwnerNameViewModel

    private val navArgs by navArgs<OwnerNameFragmentArgs>()
    private val ownerAddress by lazy { navArgs.ownerAddress.asEthereumAddress()!! }
    private val ownerKey by lazy { navArgs.ownerKey.hexAsBigInteger() }
    private val fromSeedPhrase by lazy { navArgs.fromSeedPhrase }

    override fun screenId() = ScreenId.OWNER_NAME

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOwnerNameBinding =
        FragmentOwnerNameBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            newAddressBlockies.setAddress(ownerAddress)
            newAddressHex.text = ownerAddress.formatEthAddress(requireContext(), addMiddleLinebreak = false)
            backButton.setOnClickListener { findNavController().navigateUp() }
            importButton.setOnClickListener {
                lifecycleScope.launch {
                    ownerNameLayout.isErrorEnabled = false
                    viewModel.importOwner(ownerAddress, ownerNameEntry.text.toString(), ownerKey, fromSeedPhrase)
                    findNavController().popBackStack(R.id.ownerInfoFragment, true)
                    //TODO: handle case when owner was added from owner list screen
                    findNavController().currentBackStackEntry?.savedStateHandle?.set(SafeOverviewBaseFragment.OWNER_IMPORT_RESULT, true)
                }
            }
            ownerNameEntry.doOnTextChanged { text, _, _, _ -> binding.importButton.isEnabled = !text.isNullOrBlank() }
        }
    }
}