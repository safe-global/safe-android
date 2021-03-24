package io.gnosis.safe.ui.settings.app.passcode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import io.gnosis.data.security.HeimdallEncryptionManager
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentPasscodeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.app.SettingsHandler
import pm.gnosis.svalinn.common.utils.showKeyboardForView
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class DisablePasscodeFragment : BaseViewBindingFragment<FragmentPasscodeBinding>() {

    override fun screenId() = ScreenId.SETTINGS_APP_PASSCODE

    @Inject
    lateinit var settingsHandler: SettingsHandler

    @Inject
    lateinit var encryptionManager: HeimdallEncryptionManager


    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPasscodeBinding =
        FragmentPasscodeBinding.inflate(inflater, container, false)

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun onResume() {
        super.onResume()
        binding.input.showKeyboardForView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            title.setText(R.string.settings_passcode_enter_passcode)

            status.visibility = View.INVISIBLE

            createPasscode.setText(R.string.settings_passcode_enter_your_current_passcode)
            backButton.setOnClickListener {
                findNavController().popBackStack(R.id.disablePasscodeFragment, true)
            }

            val digits = listOf(digit1, digit2, digit3, digit4, digit5, digit6)
            input.showKeyboardForView()

            //Disable done button
            input.setOnEditorActionListener { _, actionId, _ ->
                actionId == EditorInfo.IME_ACTION_DONE
            }

            input.doOnTextChanged { text, _, _, _ ->
                text?.let {
                    if (input.text.length < 6) {
                        digits.forEach {
                            it.background = ContextCompat.getDrawable(requireContext(), R.color.surface_01)
                        }
                        (1..text.length).forEach { i ->
                            digits[i - 1].background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_circle_passcode_filled_20dp)
                        }
                    } else {

                        //TODO verify passcode and disable passcode
                        val success = encryptionManager.unlockWithPassword(text.toString().toByteArray())
                        if (success) {
                            settingsHandler.usePasscode = false
                            tracker.setPasscodeIsSet(false)
                            tracker.logPasscodeDisabled()
                            findNavController().popBackStack(R.id.disablePasscodeFragment, true)
                            findNavController().currentBackStackEntry?.savedStateHandle?.set(SafeOverviewBaseFragment.PASSCODE_DISABLED_RESULT, true)
                        } else {
                            errorMessage.setText(R.string.settings_passcode_wrong_passcode)
                            errorMessage.visible(true)
                            input.setText("")
                        }

                    }
                }
            }

            helpText.visible(false)

            // Forgot your passcode button
            actionButton.setText(R.string.settings_passcode_forgot_your_passcode)
            actionButton.setOnClickListener {
                snackbar(requireView(), R.string.settings_passcode_reset_passcode)

                input.hideSoftKeyboard()
                findNavController().popBackStack(R.id.disablePasscodeFragment, true)
                findNavController().currentBackStackEntry?.savedStateHandle?.set(SafeOverviewBaseFragment.PASSCODE_DISABLED_RESULT, false)
            }
        }
    }
}
