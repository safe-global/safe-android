package io.gnosis.safe.ui.settings.app.passcode

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentPasscodeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import pm.gnosis.svalinn.common.utils.showKeyboardForView
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class ChangeRepeatPasscodeFragment : BaseViewBindingFragment<FragmentPasscodeBinding>() {

    override fun screenId() = ScreenId.PASSCODE_CHANGE_REPEAT

    private val navArgs by navArgs<ChangeRepeatPasscodeFragmentArgs>()
    private val passcodeArg by lazy { navArgs.passcode }
    private val oldPasscode by lazy { navArgs.oldPasscode }

    @Inject
    lateinit var viewModel: PasscodeViewModel

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPasscodeBinding =
        FragmentPasscodeBinding.inflate(inflater, container, false)

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun onResume() {
        super.onResume()
        binding.input.setRawInputType(InputType.TYPE_CLASS_NUMBER)
        binding.input.delayShowKeyboardForView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.state.observe(viewLifecycleOwner, Observer {
            when (val viewAction = it.viewAction) {
                is PasscodeViewModel.PasscodeWrong -> {
                    binding.errorMessage.setText(R.string.settings_passcode_wrong_passcode)
                    binding.errorMessage.visible(true)
                    binding.input.setText("")
                }
                is PasscodeViewModel.PasscodeChanged -> {
                    findNavController().popBackStack(R.id.changePasscodeFragment, true)
                    findNavController().currentBackStackEntry?.savedStateHandle?.set(SafeOverviewBaseFragment.PASSCODE_CHANGED_RESULT, true)
                }
            }
        })

        with(binding) {
            title.setText(R.string.settings_passcode_change_passcode)

            createPasscode.setText(R.string.settings_passcode_repeat_the_6_digit_passcode)

            backButton.setOnClickListener {
                findNavController().popBackStack(R.id.changeCreatePasscodeFragment, true)
            }

            status.visibility = View.INVISIBLE

            val digits = listOf(digit1, digit2, digit3, digit4, digit5, digit6)

            //Disable done button
            input.setOnEditorActionListener { _, actionId, _ ->
                actionId == EditorInfo.IME_ACTION_DONE
            }
            input.doOnTextChanged(onSixDigitsHandler(digits, requireContext()) { digitsAsString ->
                if (digitsAsString == passcodeArg) {
                    if (requireContext().canAuthenticateUsingBiometrics() == BiometricManager.BIOMETRIC_SUCCESS) {
                        viewModel.encryptPasscodeWithBiometricKey(digitsAsString)
                    }
                    viewModel.disableAndSetNewPasscode(newPasscode = passcodeArg, oldPasscode = oldPasscode)
                } else {
                    errorMessage.visible(true)
                    input.setText("")
                    digits.forEach { it.background = ContextCompat.getDrawable(requireContext(), R.color.background_secondary) }
                }
            })

            actionButton.visible(false)
            rootView.setOnClickListener {
                input.showKeyboardForView()
            }
        }
    }
}
