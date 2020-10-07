package io.gnosis.safe.ui.settings.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.BuildConfig
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsAppBinding
import io.gnosis.safe.databinding.ItemImportOwnerKeyBinding
import io.gnosis.safe.databinding.ItemRemoveOwnerKeyBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.SettingsFragmentDirections
import io.gnosis.safe.utils.OwnerKeyHandler
import io.gnosis.safe.utils.shortChecksumString
import io.gnosis.safe.utils.showRemoveDialog
import pm.gnosis.svalinn.common.utils.openUrl
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

class AppSettingsFragment : BaseViewBindingFragment<FragmentSettingsAppBinding>() {

    @Inject
    lateinit var safeRepository: SafeRepository

    @Inject
    lateinit var ownerKeyHandler: OwnerKeyHandler

    override fun screenId() = ScreenId.SETTINGS_APP

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsAppBinding =
            FragmentSettingsAppBinding.inflate(inflater, container, false)

    private lateinit var ownerKeyStubBinding: ViewBinding


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            setupOwnerKeyView()
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
    }

    private fun setupOwnerKeyView() {
        with(binding) {
            //TODO: check if app has owner key saved instead of using random

            val address = ownerKeyHandler.retrieveOwnerAddress()
            val key = ownerKeyHandler.retrieveKey()

            if (address != null && key != BigInteger.ZERO) {
                val viewStub = stubRemoveOwnerKey
                if (viewStub.parent != null) {
                    ownerKeyStubBinding = ItemRemoveOwnerKeyBinding.bind(viewStub.inflate())
                }
                with(ownerKeyStubBinding as ItemRemoveOwnerKeyBinding) {
                    blockies.setAddress(address)
                    ownerAddress.text = address.shortChecksumString()
                    remove.setOnClickListener {
                        showRemoveDialog(requireContext(), R.string.signing_owner_dialog_description) {
                            //TODO: remove owner key

                            Timber.i("---> Remove owner key")
                            ownerKeyHandler.storeKey(BigInteger.ZERO)
                            ownerKeyHandler.storeOwnerAddress(null)

                            // TODO: refresh view to show import owner key after deletion

                            // refreshView()
                        }
                    }
//                    root.setOnClickListener {
//
//                    }
                }
            } else {
                val viewStub = stubImportOwnerKey
                if (viewStub.parent != null) {
                    ownerKeyStubBinding = ItemImportOwnerKeyBinding.bind(viewStub.inflate())
                }
                with(ownerKeyStubBinding as ItemImportOwnerKeyBinding) {
                    //TODO: navigate to seed phrase import instead
                    importOwnerKey.setOnClickListener {
                        findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToImportOwnerKeyFragment())
                    }
                }
            }
        }
    }

    companion object {

        fun newInstance(): AppSettingsFragment {
            return AppSettingsFragment()
        }
    }
}
