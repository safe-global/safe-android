package io.gnosis.safe.ui.settings.app.passcode

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentPasscodeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.ui.settings.owner.details.OwnerDetailsFragment
import io.gnosis.safe.utils.setToCurrent
import io.gnosis.safe.utils.showConfirmDialog
import pm.gnosis.svalinn.common.utils.showKeyboardForView
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class EnterPasscodeFragment : BaseViewBindingFragment<FragmentPasscodeBinding>() {

    override fun screenId() = ScreenId.PASSCODE_ENTER
    private val navArgs by navArgs<EnterPasscodeFragmentArgs>()
    private val selectedOwner by lazy { navArgs.selectedOwner }
    private val requirePasscodeToOpen by lazy { navArgs.requirePasscodeToOpen }

    @Inject
    lateinit var viewModel: PasscodeViewModel

    @Inject
    lateinit var settingsHandler: SettingsHandler

    private var forgotPasscodeDialog: Dialog? = null

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPasscodeBinding =
        FragmentPasscodeBinding.inflate(inflater, container, false)

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun onResume() {
        super.onResume()
        binding.input.delayShowKeyboardForView()
        authenticateWithBiometrics()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.state.observe(viewLifecycleOwner, Observer {
            when (val viewAction = it.viewAction) {
                is PasscodeViewModel.AllOwnersRemoved -> {
                    snackbar(requireView(), R.string.passcode_disabled)
                    if (requirePasscodeToOpen) {
                        findNavController().popBackStack(R.id.enterPasscodeFragment, true)
                    } else if (!selectedOwner.isNullOrBlank()) {
                        findNavController().popBackStack(R.id.transactionDetailsFragment, true)
                    } else {
                        findNavController().popBackStack(R.id.ownerDetailsFragment, true)
                    }
                }
                is BaseStateViewModel.ViewAction.ShowError -> {
                    binding.errorMessage.setText(R.string.settings_passcode_owner_removal_failed)
                    binding.errorMessage.visible(true)
                }
                is PasscodeViewModel.PasscodeWrong -> {
                    binding.errorMessage.setText(R.string.settings_passcode_wrong_passcode)
                    binding.errorMessage.visible(true)
                    binding.input.setText("")
                }
                is PasscodeViewModel.PasscodeCorrect -> {
                    handlePasscodeCorrect()
                }
            }
        })

        with(binding) {
            title.setText(R.string.settings_passcode_enter_passcode)
            createPasscode.setText(R.string.settings_passcode_enter_your_current_passcode)
            helpText.visible(false)

            if (requirePasscodeToOpen) {
                backButton.visible(false)
                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        input.delayShowKeyboardForView()
                    }
                })
            } else {
                backButton.setOnClickListener {
                    findNavController().navigateUp()
                }
            }

            status.visibility = View.INVISIBLE

            val digits = listOf(digit1, digit2, digit3, digit4, digit5, digit6)
            input.showKeyboardForView()

            //Disable done button
            input.setOnEditorActionListener { _, actionId, _ ->
                actionId == EditorInfo.IME_ACTION_DONE
            }

            input.doOnTextChanged(onSixDigitsHandler(digits, requireContext()) { digitsAsString ->
                viewModel.unlockWithPasscode(digitsAsString)
            })

            errorMessage.setText(R.string.settings_passcode_wrong_passcode)
            actionButton.setText(R.string.settings_passcode_forgot_your_passcode)
            actionButton.setOnClickListener {
                binding.errorMessage.visibility = View.INVISIBLE

                forgotPasscodeDialog = showConfirmDialog(
                    requireContext(),
                    message = R.string.settings_passcode_confirm_disable_passcode,
                    confirm = R.string.settings_passcode_disable_passcode,
                    confirmColor = R.color.error
                ) {
                    viewModel.onForgotPasscode()
                    input.hideSoftKeyboard()
                }
            }
            rootView.setOnClickListener {
                input.showKeyboardForView()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        forgotPasscodeDialog?.dismiss()
    }

    private fun handlePasscodeCorrect() {
        if (requirePasscodeToOpen) {
            findNavController().popBackStack(R.id.enterPasscodeFragment, true)
            binding.input.hideSoftKeyboard()
        } else if (!selectedOwner.isNullOrBlank()) {
            findNavController().popBackStack(R.id.signingOwnerSelectionFragment, true)
            findNavController().setToCurrent(SafeOverviewBaseFragment.OWNER_SELECTED_RESULT, selectedOwner)
        } else {
            // passcode for export
            findNavController().popBackStack(R.id.enterPasscodeFragment, true)
            findNavController().setToCurrent(OwnerDetailsFragment.RESULT_PASSCODE_UNLOCKED, true)
            binding.input.hideSoftKeyboard()
        }
    }

    private fun authenticateWithBiometrics() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (settingsHandler.useBiometrics) {
                val biometricPrompt = createBiometricPrompt(
                    fragment = this@EnterPasscodeFragment,
                    authCallback = object : BiometricPrompt.AuthenticationCallback() {

                        override fun onAuthenticationError(errCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errCode, errString)
                            if (errCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                onUsePasscode()
                            }
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            onBiometricsAuthFailed()
                        }

                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            onBiometricsSuccess(result)
                        }
                    }
                )
                val promptInfo = if (requirePasscodeToOpen) {
                    createPromptInfo(requireContext(), R.string.biometric_prompt_info_subtitle_login)
                } else {
                    createPromptInfo(requireContext(), R.string.biometric_prompt_info_subtitle_sign)
                }
                viewModel.biometricAuthentication(biometricPrompt, promptInfo)
            }
        }
    }

    private fun onUsePasscode() {
        binding.input.delayShowKeyboardForView()
    }

    private fun onBiometricsAuthFailed() {
        binding.input.delayShowKeyboardForView()
    }

    private fun onBiometricsSuccess(authenticationResult: BiometricPrompt.AuthenticationResult) {
        if (requirePasscodeToOpen) {
            findNavController().popBackStack(R.id.enterPasscodeFragment, true)
            binding.input.hideSoftKeyboard()
        } else {
            viewModel.decryptPasscode(authenticationResult)
            binding.input.delayShowKeyboardForView()
        }
    }

    private fun createBiometricPrompt(
        fragment: Fragment,
        authCallback: BiometricPrompt.AuthenticationCallback
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(fragment.requireContext())
        return BiometricPrompt(fragment, executor, authCallback)
    }

    private fun createPromptInfo(context: Context, @StringRes subtitle: Int): BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo.Builder().apply {
            setTitle(context.getString(R.string.biometric_prompt_info_title))
            setSubtitle(context.getString(subtitle))
            setConfirmationRequired(true)
            setNegativeButtonText(context.getString(R.string.biometric_prompt_info_use_app_password))
        }.build()
}
