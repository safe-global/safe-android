package io.gnosis.safe.ui.settings.app.passcode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsAppPasscodeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.app.SettingsHandler
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class PasscodeSettingsFragment : BaseViewBindingFragment<FragmentSettingsAppPasscodeBinding>() {

    override fun screenId() = ScreenId.SETTINGS_APP_PASSCODE

    @Inject
    lateinit var settingsHandler: SettingsHandler

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsAppPasscodeBinding =
        FragmentSettingsAppPasscodeBinding.inflate(inflater, container, false)

    companion object {
        fun newInstance() = PasscodeSettingsFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
            }

            usePasscode.settingSwitch.isChecked = settingsHandler.usePasscode
            usePasscode.settingSwitch.setOnClickListener {
                settingsHandler.usePasscode = usePasscode.settingSwitch.isChecked

                //TODO Start passcode setup flow here

                changePasscode.visible(settingsHandler.usePasscode)
            }
            changePasscode.visible(settingsHandler.usePasscode)
        }
    }
}
