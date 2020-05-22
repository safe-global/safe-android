package io.gnosis.safe.ui.safe.settings.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.BuildConfig
import io.gnosis.safe.R
import io.gnosis.safe.databinding.FragmentSettingsAppBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment
import io.gnosis.safe.ui.safe.settings.SettingsFragmentDirections
import pm.gnosis.svalinn.common.utils.openUrl
import javax.inject.Inject

class AppSettingsFragment: BaseFragment<FragmentSettingsAppBinding>() {

    @Inject
    lateinit var viewModel: AppSettingsViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsAppBinding =
        FragmentSettingsAppBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
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

    companion object {

        fun newInstance(): AppSettingsFragment {
            return AppSettingsFragment()
        }
    }
}
