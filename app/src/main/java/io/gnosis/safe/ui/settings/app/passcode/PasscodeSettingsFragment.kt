package io.gnosis.safe.ui.settings.app.passcode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import io.gnosis.data.models.Safe
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsAppPasscodeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment
import io.gnosis.safe.ui.settings.app.SettingsHandler
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class PasscodeSettingsFragment : SafeOverviewBaseFragment<FragmentSettingsAppPasscodeBinding>() {

    override fun screenId() = ScreenId.SETTINGS_APP_PASSCODE

    @Inject
    lateinit var settingsHandler: SettingsHandler

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsAppPasscodeBinding =
        FragmentSettingsAppPasscodeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
            }

            usePasscode.settingSwitch.isChecked = settingsHandler.usePasscode
            usePasscode.settingSwitch.setOnClickListener {
                if (settingsHandler.usePasscode) {
//                    settingsHandler.usePasscode = false
                    updateUi()
                    findNavController().navigate(PasscodeSettingsFragmentDirections.actionPasscodeSettingsFragmentToDisablePasscodeFragment())
                } else {
                    settingsHandler.requireToOpen = true
                    settingsHandler.requireForConfirmations = true
                    findNavController().navigate(
                        PasscodeSettingsFragmentDirections.actionPasscodeSettingsFragmentToCreatePasscodeFragment(ownerImported = false)
                    )
                }
                changePasscode.visible(settingsHandler.usePasscode)
            }
            changePasscode.visible(settingsHandler.usePasscode)
            changePasscode.setOnClickListener {
                findNavController().navigate(
                    PasscodeSettingsFragmentDirections.actionPasscodeSettingsFragmentToChangePasscodeFragment()
                )
            }

            useBiometrics.visible(settingsHandler.usePasscode)
            useBiometrics.settingSwitch.isChecked = settingsHandler.useBiometrics
            useBiometrics.settingSwitch.setOnClickListener {
                settingsHandler.useBiometrics = useBiometrics.settingSwitch.isChecked
                //TODO de/activate biometric for app/confirmations
            }

            usePasscodeFor.visible(settingsHandler.usePasscode)

            requireToOpen.visible(settingsHandler.usePasscode)
            requireToOpen.settingSwitch.isChecked = settingsHandler.requireToOpen
            requireToOpen.settingSwitch.setOnClickListener {
                settingsHandler.requireToOpen = requireToOpen.settingSwitch.isChecked
                // If both are disabled, disable passcode feature
                if (!settingsHandler.requireForConfirmations && !settingsHandler.requireToOpen) {
                    settingsHandler.usePasscode = false
                    updateUi()
                    findNavController().navigate(PasscodeSettingsFragmentDirections.actionPasscodeSettingsFragmentToDisablePasscodeFragment())
                }
            }

            requireForConfirmations.visible(settingsHandler.usePasscode)
            requireForConfirmations.settingSwitch.isChecked = settingsHandler.requireForConfirmations
            requireForConfirmations.settingSwitch.setOnClickListener {
                settingsHandler.requireForConfirmations = requireForConfirmations.settingSwitch.isChecked
                // If both are disabled, disable passcode feature
                if (!settingsHandler.requireForConfirmations && !settingsHandler.requireToOpen) {
                    settingsHandler.usePasscode = false
                    updateUi()
                    findNavController().navigate(PasscodeSettingsFragmentDirections.actionPasscodeSettingsFragmentToDisablePasscodeFragment())

                }
            }
        }
    }

    private fun updateUi(usePasscode: Boolean = false) {
        with(binding) {
            changePasscode.visible(usePasscode)
            useBiometrics.visible(usePasscode)
            usePasscodeFor.visible(usePasscode)
            requireToOpen.visible(usePasscode)
            requireForConfirmations.visible(usePasscode)
            usePasscodeFor.visible(usePasscode)
        }
    }

    override fun handleActiveSafe(safe: Safe?) {
        // ignored for now
    }

    override fun onResume() {
        super.onResume()
        binding.usePasscode.settingSwitch.isChecked = settingsHandler.usePasscode
    }
}
