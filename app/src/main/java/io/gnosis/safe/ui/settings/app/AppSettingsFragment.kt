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
import java.math.BigInteger
import javax.inject.Inject

class AppSettingsFragment : BaseViewBindingFragment<FragmentSettingsAppBinding>() {

    @Inject
    lateinit var viewModel: AppSettingsViewModel

    override fun screenId() = ScreenId.SETTINGS_APP

    override suspend fun chainId(): BigInteger? = null

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
            intercom.settingImage = R.drawable.ic_settings_intercom_notification_24dp
            intercom.setOnClickListener {
                viewModel.openIntercomMessenger()
            }
            helpCenter.setOnClickListener {
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToGetInTouchFragment())
            }
            advanced.setOnClickListener {
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToAdvancedAppSettingsFragment())
            }
            aboutSafe.setOnClickListener {
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToAboutSafeFragment())
            }
            version.value = BuildConfig.VERSION_NAME
        }

        viewModel.signingOwnerCount.observe(viewLifecycleOwner, Observer {
            binding.ownerKeys.value = it.toString()
        })
        viewModel.loadSigningOwner()

        viewModel.defaultFiat.observe(viewLifecycleOwner, Observer {
            binding.fiat.value = it
        })
        viewModel.loadUserDefaultFiat()

        viewModel.intercomCount.observe(viewLifecycleOwner, {
            updateIntercomIcon(it)
        })
        viewModel.loadIntercomCount()
    }

    private fun updateIntercomIcon(count: Int) {
        with(binding) {
            intercom.settingImage = if (count > 0) R.drawable.ic_settings_intercom_notification_24dp else R.drawable.ic_settings_intercom_24dp
        }
    }

    companion object {

        fun newInstance(): AppSettingsFragment {
            return AppSettingsFragment()
        }
    }
}
