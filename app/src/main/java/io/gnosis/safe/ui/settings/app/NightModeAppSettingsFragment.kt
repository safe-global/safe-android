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
    override fun screenId() = ScreenId.SETTINGS_APP_APPEARANCE

    @Inject
    lateinit var settingsHandler: SettingsHandler

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
            when (settingsHandler.nightMode) {
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
                settingsHandler.nightMode = MODE_NIGHT_FOLLOW_SYSTEM
                settingsHandler.applyNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
            }
            light.setOnClickListener {
                lightChecked()
                settingsHandler.nightMode = MODE_NIGHT_NO
                settingsHandler.applyNightMode(MODE_NIGHT_NO)
            }
            dark.setOnClickListener {
                darkChecked()
                settingsHandler.nightMode = MODE_NIGHT_YES
                settingsHandler.applyNightMode(MODE_NIGHT_YES)
            }
        }
    }

    private fun darkChecked() {
        with(binding) {
            auto.checked = false
            light.checked = false
            dark.checked = true
        }
    }

    private fun lightChecked() {
        with(binding) {
            auto.checked = false
            light.checked = true
            dark.checked = false
        }
    }

    private fun autoChecked() {
        with(binding) {
            auto.checked = true
            light.checked = false
            dark.checked = false
        }
    }
}
