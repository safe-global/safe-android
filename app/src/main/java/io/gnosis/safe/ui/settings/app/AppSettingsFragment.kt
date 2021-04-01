package io.gnosis.safe.ui.settings.app

import android.content.Intent
import android.net.Uri
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
import pm.gnosis.svalinn.common.utils.openUrl
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
            ownerKeys.setOnClickListener {
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToOwnerListFragment())
            }
            appearance.setOnClickListener {
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToNightModeSettingsFragment())
            }
            passcode.setOnClickListener {
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToPasscodeSettingsFragment())
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
            rateApp.setOnClickListener {
                openPlayStore()
            }
            version.value = BuildConfig.VERSION_NAME
            network.value = BuildConfig.BLOCKCHAIN_NAME
            advanced.setOnClickListener {
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToAdvancedAppSettingsFragment())
            }
        }

        viewModel.signingOwnerCount.observe(viewLifecycleOwner, Observer {
            binding.ownerKeys.value = it.toString()
        })

        viewModel.loadSigningOwner()

        viewModel.defaultFiat.observe(viewLifecycleOwner, Observer {
            binding.fiat.value = it
        })

        viewModel.loadUserDefaultFiat()
    }

    private fun openPlayStore() {
        kotlin.runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${requireActivity().packageName}")))
        }
            .onFailure {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store.apps/details?id=${requireActivity().packageName}")))
            }
    }

    companion object {

        fun newInstance(): AppSettingsFragment {
            return AppSettingsFragment()
        }
    }
}
