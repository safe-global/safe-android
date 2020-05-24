package io.gnosis.safe.ui.safe.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import io.gnosis.data.models.Safe
import io.gnosis.safe.R
import io.gnosis.safe.databinding.FragmentSettingsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.safe.SafeOverviewBaseFragment
import io.gnosis.safe.ui.safe.empty.NoSafeFragment
import io.gnosis.safe.ui.safe.settings.app.AppSettingsFragment
import io.gnosis.safe.ui.safe.settings.safe.SafeSettingsFragment
import javax.inject.Inject

class SettingsFragment : SafeOverviewBaseFragment<FragmentSettingsBinding>() {

    @Inject
    lateinit var viewModel: SettingsViewModel

    private lateinit var pager: SettingsPagerAdapter

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsBinding =
        FragmentSettingsBinding.inflate(inflater, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            pager = SettingsPagerAdapter(this@SettingsFragment)
            settingsContent.adapter = pager
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

        viewModel.state.observe(viewLifecycleOwner, Observer {state ->
            when (state) {
                is SettingsState.ActiveSafe -> {
                    pager.noActiveSafe = false
                    handleActiveSafe(state.safe)
                }
                is SettingsState.NoActiveSafe -> {
                    pager.noActiveSafe = true
                    handleActiveSafe(null)
                }
            }
        })
    }

    override fun handleActiveSafe(safe: Safe?) {
        navHandler?.setSafeData(safe)
    }
}

class SettingsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    enum class Tabs { SAFE, APP }

    var noActiveSafe: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = Tabs.values().size

    override fun createFragment(position: Int): Fragment =
        when (Tabs.values()[position]) {
            Tabs.SAFE -> if (noActiveSafe) NoSafeFragment.newInstance() else SafeSettingsFragment.newInstance()
            Tabs.APP -> AppSettingsFragment.newInstance()
        }

    // item ids:
    // SAFE -> 1
    // APP -> 2
    // NO_SAFE -> 3
    override fun getItemId(position: Int): Long {
        return when (Tabs.values()[position]) {
            Tabs.SAFE -> if (noActiveSafe) 3L else 1L
            Tabs.APP -> 2L
        }
    }

    override fun containsItem(itemId: Long): Boolean {
        return when {
            noActiveSafe && itemId == 3L -> true
            noActiveSafe && itemId != 3L -> false
            else -> itemId == 1L || itemId == 2L
        }
    }
}
