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
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.utils.BlockExplorer
import io.gnosis.safe.utils.formatEthAddress
import io.gnosis.safe.utils.toColor
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.svalinn.common.utils.copyToClipboard
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.utils.asEthereumAddress
import javax.inject.Inject

class AddSafeOwnerFragment : BaseViewBindingFragment<FragmentAddSafeOwnerBinding>() {

    private val navArgs by navArgs<AddSafeOwnerFragmentArgs>()
    private val name by lazy { navArgs.safeName }
    private val address by lazy { navArgs.safeAddress.asEthereumAddress()!! }
    private val selectedChain by lazy { navArgs.chain }

    @Inject
    lateinit var settingsHandler: SettingsHandler

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
            safeAddress.text = address.formatEthAddress(
                context = requireContext(),
                chainPrefix = if (settingsHandler.chainPrefixPrepend) selectedChain.shortName else null,
                addMiddleLinebreak = false
            )
            safeAddress.setOnClickListener {
                requireContext().copyToClipboard(getString(R.string.address_copied), address.asEthereumAddressChecksumString()) {
                    snackbar(view = root, textId = R.string.copied_success)
                }
            }
            safeLink.setOnClickListener {
                BlockExplorer.forChain(selectedChain)?.showAddress(requireContext(), address)
            }
            blockie.setAddress(address)
            readOnlyDescription.text = getString(R.string.add_safe_owner_read_only_notice, name)
            addOwnerButton.setOnClickListener {
                tracker.logOnboardingOwnerImport()
                finishAddSafeFlow()
                findNavController().navigate(R.id.action_to_add_owner)
            }
            skip.setOnClickListener {
                tracker.logOnboardingOwnerSkipped()
                finishAddSafeFlow()
            }
            chainRibbon.text = selectedChain.name
            chainRibbon.setTextColor(selectedChain.textColor.toColor(requireContext(), R.color.white))
            chainRibbon.setBackgroundColor(selectedChain.backgroundColor.toColor(requireContext(), R.color.primary))
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAddSafeFlow()
            }
        })

        settingsHandler.showOwnerScreen = false
    }

    private fun finishAddSafeFlow() {
        findNavController().popBackStack(R.id.selectChainFragment, true)
    }
}
