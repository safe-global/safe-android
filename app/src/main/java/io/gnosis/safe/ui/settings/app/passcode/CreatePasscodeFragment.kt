package io.gnosis.safe.ui.settings.app.passcode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsCreatePasscodeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment

class CreatePasscodeFragment : BaseViewBindingFragment<FragmentSettingsCreatePasscodeBinding>() {

    override fun screenId() = ScreenId.CREATE_PASSCODE

    companion object {
        fun newInstance() = CreatePasscodeFragment()
    }

//    private lateinit var viewModel: CreatePasscodeViewModel

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsCreatePasscodeBinding =
        FragmentSettingsCreatePasscodeBinding.inflate(inflater, container, false)

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        //TODO Open numeric keyboard
        //Reopen it when one of the circles is touched

        with(binding) {
            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
            }

//            usePasscode.settingSwitch.isChecked = settingsHandler.usePasscode
//            usePasscode.settingSwitch.setOnClickListener {
//                settingsHandler.usePasscode = usePasscode.settingSwitch.isChecked
//
//                //TODO Start passcode setup flow here
//
//                changePasscode.visible(settingsHandler.usePasscode)
//            }
//            changePasscode.visible(settingsHandler.usePasscode)
        }
    }
}
