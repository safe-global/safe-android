package io.gnosis.safe.ui.settings.app

import android.view.LayoutInflater
import android.view.ViewGroup
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsAppDarkNightBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment

class DarkNightAppSettingsFragment : BaseViewBindingFragment<FragmentSettingsAppDarkNightBinding>() {
    override fun screenId() = ScreenId.SETTINGS_APP_ADVANCED

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsAppDarkNightBinding =
        FragmentSettingsAppDarkNightBinding.inflate(inflater, container, false)
}
