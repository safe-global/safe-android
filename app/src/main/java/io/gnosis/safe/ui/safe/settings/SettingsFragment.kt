package io.gnosis.safe.ui.safe.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import io.gnosis.data.models.Safe
import io.gnosis.safe.R
import io.gnosis.safe.databinding.FragmentSettingsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction
import io.gnosis.safe.ui.safe.SafeOverviewBaseFragment
import io.gnosis.safe.ui.safe.settings.app.AppSettingsFragment
import io.gnosis.safe.ui.safe.settings.safe.SafeSettingsFragment
import pm.gnosis.svalinn.common.utils.snackbar
import javax.inject.Inject

class SettingsFragment : SafeOverviewBaseFragment<FragmentSettingsBinding>() {

    @Inject
    lateinit var viewModel: SettingsViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsBinding =
        FragmentSettingsBinding.inflate(inflater, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            val adapter = SettingsPagerAdapter(this@SettingsFragment)
            settingsContent.adapter = adapter
            TabLayoutMediator(settingsTabBar, settingsContent, true) { tab, position ->
                when (SettingsPagerAdapter.Tabs.values()[position]) {
                    SettingsPagerAdapter.Tabs.SAFE -> {
                        tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_settings_safe_24dp)
                        tab.text = getString(R.string.settings_tab_safe)
                    }
                    SettingsPagerAdapter.Tabs.APP -> {
                        tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_settings_app_24dp)
                        tab.text = getString(R.string.settings_tab_app)
                    }
                }
            }.attach()
        }

        viewModel.state.observe(viewLifecycleOwner, Observer {

            when (val viewAction = it.viewAction) {

                is ViewAction.Loading -> {

                }

                is ViewAction.ShowError -> {
                    snackbar(binding.root, "safe not loaded!")
                }

                is ViewAction.NavigateTo -> {
                    findNavController().navigate(viewAction.navDirections)
                }

                is ViewAction.UpdateActiveSafe -> {
                    handleActiveSafe(viewAction.newSafe)
                    if (viewAction.newSafe == null) {
                        findNavController().popBackStack()
                    }
                }
            }
        })
    }

    override fun handleActiveSafe(safe: Safe?) {
        navHandler?.setSafeData(safe)
    }
}

class SettingsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    enum class Tabs { APP, SAFE }

    override fun getItemCount(): Int = Tabs.values().size

    override fun createFragment(position: Int): Fragment =
        when (Tabs.values()[position]) {
            Tabs.APP -> AppSettingsFragment.newInstance()
            Tabs.SAFE -> SafeSettingsFragment.newInstance()
        }
}
