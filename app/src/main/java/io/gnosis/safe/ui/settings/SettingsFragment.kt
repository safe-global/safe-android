package io.gnosis.safe.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import io.gnosis.data.models.Safe
import io.gnosis.safe.R
import io.gnosis.safe.databinding.FragmentSettingsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment
import io.gnosis.safe.ui.safe.empty.NoSafeFragment
import io.gnosis.safe.ui.settings.app.AppSettingsFragment
import io.gnosis.safe.ui.settings.safe.SafeSettingsFragment
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

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (state.viewAction) {
                is BaseStateViewModel.ViewAction.Loading -> {
                }
                else -> {
                    handleActiveSafe(state.safe)
                    pager.noActiveSafe = state.safe == null
                }
            }
        })
    }

    override fun handleActiveSafe(safe: Safe?) {
        navHandler?.setSafeData(safe)
    }

    override fun screenId() = null
}

class SettingsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    enum class Tabs { APP, SAFE }

    enum class Items(val value: Long) {
        APP(Tabs.APP.ordinal.toLong()),
        SAFE(Tabs.SAFE.ordinal.toLong()),
        NO_SAFE(RecyclerView.NO_ID) }

    var noActiveSafe: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = Tabs.values().size

    override fun createFragment(position: Int): Fragment =
        when (Tabs.values()[position]) {
            Tabs.APP -> AppSettingsFragment.newInstance()
            Tabs.SAFE -> if (noActiveSafe) NoSafeFragment.newInstance(NoSafeFragment.Position.SETTINGS) else SafeSettingsFragment.newInstance()
        }

    override fun getItemId(position: Int): Long {
        return when (Tabs.values()[position]) {
            Tabs.APP -> Items.APP.value
            Tabs.SAFE -> if (noActiveSafe) Items.NO_SAFE.value else Items.SAFE.value
        }
    }

    override fun containsItem(itemId: Long): Boolean {
        return when (itemId) {
            Items.NO_SAFE.value -> noActiveSafe
            Items.SAFE.value -> !noActiveSafe
            else -> true
        }
    }
}
