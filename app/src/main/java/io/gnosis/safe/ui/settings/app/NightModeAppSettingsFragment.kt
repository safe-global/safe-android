package io.gnosis.safe.ui.settings.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsAppNightModeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import javax.inject.Inject

class NightModeAppSettingsFragment : BaseViewBindingFragment<FragmentSettingsAppNightModeBinding>() {
    override fun screenId() = ScreenId.SETTINGS_APP_ADVANCED

    @Inject
    lateinit var appearanceSettingsHandler: AppearanceSettingsHandler

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsAppNightModeBinding =
        FragmentSettingsAppNightModeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            backButton.setOnClickListener {
                findNavController().navigateUp()
            }
            when (appearanceSettingsHandler.nightMode) {
                MODE_NIGHT_YES -> {
                    darkChecked()
                }
                MODE_NIGHT_NO -> {
                    lightChecked()
                }
                else -> {
                    autoChecked()
                }
            }
            auto.setOnClickListener {
                autoChecked()
                appearanceSettingsHandler.nightMode = MODE_NIGHT_FOLLOW_SYSTEM
                appearanceSettingsHandler.applyNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
            }
            light.setOnClickListener {
                lightChecked()
                appearanceSettingsHandler.nightMode = MODE_NIGHT_NO
                appearanceSettingsHandler.applyNightMode(MODE_NIGHT_NO)
            }
            dark.setOnClickListener {
                darkChecked()
                appearanceSettingsHandler.nightMode = MODE_NIGHT_YES
                appearanceSettingsHandler.applyNightMode(MODE_NIGHT_YES)
            }
        }
    }

    private fun FragmentSettingsAppNightModeBinding.darkChecked() {
        auto.checked = false
        light.checked = false
        dark.checked = true
    }

    private fun FragmentSettingsAppNightModeBinding.lightChecked() {
        auto.checked = false
        light.checked = true
        dark.checked = false
    }

    private fun FragmentSettingsAppNightModeBinding.autoChecked() {
        auto.checked = true
        light.checked = false
        dark.checked = false
    }
}
