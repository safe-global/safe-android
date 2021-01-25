package io.gnosis.safe.ui.safe.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentAddSafeOwnerBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.assets.AssetsFragmentDirections
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.formatEthAddress
import pm.gnosis.utils.asEthereumAddress

class AddSafeOwnerFragment : BaseViewBindingFragment<FragmentAddSafeOwnerBinding>() {

    private val navArgs by navArgs<AddSafeOwnerFragmentArgs>()
    private val name by lazy { navArgs.safeName }
    private val address by lazy { navArgs.safeAddress.asEthereumAddress()!! }

    override fun screenId() = ScreenId.SAFE_ADD_OWNER

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddSafeOwnerBinding =
        FragmentAddSafeOwnerBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            backButton.setOnClickListener { finishAddSafeFlow() }
            safeName.text = name
            safeAddress.text = address.formatEthAddress(requireContext(), addMiddleLinebreak = false)
            blockie.setAddress(address)
            readOnlyDescription.text = getString(R.string.add_safe_owner_read_only_notice, name)
            importButton.setOnClickListener {
                tracker.logOnboardingOwnerImport()
                finishAddSafeFlow()
                findNavController().navigate(AssetsFragmentDirections.actionAssetsFragmentToOwnerInfoFragment())
            }
            skip.setOnClickListener {
                tracker.logOnboardingOwnerSkipped()
                finishAddSafeFlow()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAddSafeFlow()
            }
        })
    }

    private fun finishAddSafeFlow() {
        findNavController().popBackStack(R.id.addSafeFragment, true)
    }
}
