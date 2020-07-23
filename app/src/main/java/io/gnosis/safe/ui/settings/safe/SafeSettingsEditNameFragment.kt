package io.gnosis.safe.ui.settings.safe

import android.view.LayoutInflater
import android.view.ViewGroup
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsSafeEditNameBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment

class SafeSettingsEditNameFragment : BaseViewBindingFragment<FragmentSettingsSafeEditNameBinding>() {

    override fun screenId() = ScreenId.SETTINGS_SAFE_EDIT_NAME

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsSafeEditNameBinding =
        FragmentSettingsSafeEditNameBinding.inflate(inflater, container, false)

}
