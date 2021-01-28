package io.gnosis.safe.ui.settings.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.BuildConfig
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsAppBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.SettingsFragmentDirections
import io.gnosis.safe.utils.shortChecksumString
import io.gnosis.safe.utils.showConfirmDialog
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.copyToClipboard
import pm.gnosis.svalinn.common.utils.openUrl
import pm.gnosis.svalinn.common.utils.snackbar
import javax.inject.Inject

class AppSettingsFragment : BaseViewBindingFragment<FragmentSettingsAppBinding>() {

    @Inject
    lateinit var viewModel: AppSettingsViewModel

    override fun screenId() = ScreenId.SETTINGS_APP

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsAppBinding =
        FragmentSettingsAppBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            setupOwnerKeyView()
            appearance.setOnClickListener {
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToNightModeSettingsFragment())
            }
            fiat.setOnClickListener {
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToAppFiatFragment())
            }
            terms.setOnClickListener {
                requireContext().openUrl(getString(R.string.link_terms_of_use))
            }
            privacy.setOnClickListener {
                requireContext().openUrl(getString(R.string.link_privacy_policy))
            }
            licenses.setOnClickListener {
                requireContext().openUrl(getString(R.string.link_licenses))
            }
            getInTouch.setOnClickListener {
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToGetInTouchFragment())
            }
            version.value = BuildConfig.VERSION_NAME
            network.value = BuildConfig.BLOCKCHAIN_NAME
            advanced.setOnClickListener {
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToAdvancedAppSettingsFragment())
            }
        }

        viewModel.signingOwner.observe(viewLifecycleOwner, Observer {
            setupOwnerKeyView(it?.address)
        })

        viewModel.loadSigningOwner()

        viewModel.defaultFiat.observe(viewLifecycleOwner, Observer {
            binding.fiat.value = it
        })

        viewModel.loadUserDefaultFiat()
    }

    private fun setupOwnerKeyView(address: Solidity.Address? = null) {
        with(binding) {
            if (address != null) {
                if (ownerKey.currentView.id == R.id.import_owner_key) {
                    ownerKey.showNext()
                }
                with(removeOwnerKey) {
                    blockies.setAddress(address)
                    ownerAddress.text = address.shortChecksumString()
                    remove.setOnClickListener {
                        showConfirmDialog(requireContext(), R.string.signing_owner_dialog_description) {
                            onOwnerRemove()
                        }
                    }
                   root.setOnClickListener {
                       address?.let {
                           context?.copyToClipboard(getString(R.string.address_copied), address.asEthereumAddressChecksumString()) {
                               snackbar(view = root, textId = R.string.copied_success)
                           }
                       }
                   }
                }
            } else {
                with(importOwnerKey) {
                    root.setOnClickListener {
                        findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToOwnerInfoFragment())
                    }
                }
            }
        }
    }

    private fun onOwnerRemove() {
        viewModel.removeSigningOwner()
        binding.ownerKey.showNext()
        snackbar(requireView(), getString(R.string.signing_owner_key_removed))
    }

    companion object {

        fun newInstance(): AppSettingsFragment {
            return AppSettingsFragment()
        }
    }
}
