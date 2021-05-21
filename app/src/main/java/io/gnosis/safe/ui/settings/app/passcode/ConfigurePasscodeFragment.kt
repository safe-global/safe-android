package io.gnosis.safe.ui.settings.app.passcode

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
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
import io.gnosis.safe.ui.settings.app.passcode.CryptographyManager.Companion.FILE_NAME
import io.gnosis.safe.ui.settings.app.passcode.CryptographyManager.Companion.KEY_NAME
import io.gnosis.safe.utils.showConfirmDialog
import pm.gnosis.svalinn.common.utils.showKeyboardForView
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class ConfigurePasscodeFragment : BaseViewBindingFragment<FragmentPasscodeBinding>() {

    override fun screenId() = ScreenId.SETTINGS_APP_PASSCODE

    @Inject
    lateinit var viewModel: PasscodeViewModel
    private val navArgs by navArgs<ConfigurePasscodeFragmentArgs>()
    private val passcodeCommand by lazy { navArgs.passcodeCommand }
    private val cm = CryptographyManager()

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
                is PasscodeViewModel.AllOwnersRemoved -> {
                    findNavController().popBackStack(R.id.configurePasscodeFragment, true)
                    findNavController().currentBackStackEntry?.savedStateHandle?.set(SafeOverviewBaseFragment.PASSCODE_DISABLED_RESULT, true)
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
                is PasscodeViewModel.PasscodeDisabled -> {
                    findNavController().popBackStack(R.id.configurePasscodeFragment, true)
                    findNavController().currentBackStackEntry?.savedStateHandle?.set(SafeOverviewBaseFragment.PASSCODE_DISABLED_RESULT, true)
                }
                is PasscodeViewModel.PasscodeCommandExecuted -> {
                    findNavController().popBackStack(R.id.configurePasscodeFragment, true)
                }
            }
        })

        with(binding) {
            title.setText(R.string.settings_passcode_enter_passcode)

            status.visibility = View.INVISIBLE

            createPasscode.setText(R.string.settings_passcode_enter_your_current_passcode)
            backButton.setOnClickListener {
                findNavController().popBackStack(R.id.configurePasscodeFragment, true)
            }

            val digits = listOf(digit1, digit2, digit3, digit4, digit5, digit6)
            input.showKeyboardForView()

            //Disable done button
            input.setOnEditorActionListener { _, actionId, _ ->
                actionId == EditorInfo.IME_ACTION_DONE
            }

            input.doOnTextChanged(onSixDigitsHandler(digits, requireContext()) { digitsAsString ->

//                if (passcodeCommand == PasscodeCommand.BIOMETRICS_ENABLE) {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                        val biometricPrompt: BiometricPrompt = createBiometricPrompt(
//                            fragment = this@ConfigurePasscodeFragment,
//                            authCallback = object : BiometricPrompt.AuthenticationCallback() {
//
//                                override fun onAuthenticationError(errCode: Int, errString: CharSequence) {
//                                    super.onAuthenticationError(errCode, errString)
//                                    if (errCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
//                                        onCancel()
//                                    }
//                                }
//
//                                override fun onAuthenticationFailed() {
//                                    super.onAuthenticationFailed()
//                                    onBiometricsAuthFailed()
//                                }
//
//                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
//                                    super.onAuthenticationSucceeded(result)
//                                    onBiometricsSuccess(result, digitsAsString)
//                                }
//                            }
//                        )
//                        val promptInfo = createPromptInfo(requireContext(), R.string.biometric_prompt_info_subtitle_enable)
//
//                        try {
//                            val cipher = cm.getInitializedCipherForEncryption(KEY_NAME)
//                            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
//                        } catch (e: KeyPermanentlyInvalidatedException) {
//                            cm.deleteKey(KEY_NAME)
//                        }
//                    }
//                } else {

                val cipher = cm.getInitializedCipherForEncryption(KEY_NAME)
                val encrypted = cm.encryptData(digitsAsString, cipher)
                cm.persistCiphertextWrapperToSharedPrefs(encrypted, requireContext(), FILE_NAME, MODE_PRIVATE, KEY_NAME)

                viewModel.configurePasscode(digitsAsString, passcodeCommand)
//                }
            })

            helpText.visible(false)

            actionButton.setText(R.string.settings_passcode_forgot_your_passcode)
            actionButton.setOnClickListener {
                binding.errorMessage.visibility = View.INVISIBLE

                showConfirmDialog(
                    requireContext(),
                    message = R.string.settings_passcode_confirm_disable_passcode,
                    confirm = R.string.settings_passcode_disable_passcode,
                    confirmColor = R.color.error
                ) {
                    viewModel.onForgotPasscode()
                    input.hideSoftKeyboard()
                }
            }
        }
    }

//    private fun onBiometricsSuccess(result: BiometricPrompt.AuthenticationResult, passcode: String) {
//        val encrypted = cm.encryptData(passcode, result.cryptoObject?.cipher!!)
//        cm.persistCiphertextWrapperToSharedPrefs(encrypted, requireContext(), FILE_NAME, MODE_PRIVATE, KEY_NAME)
//
//        viewModel.configurePasscode(passcode, PasscodeCommand.BIOMETRICS_ENABLE)
//    }
//
//    private fun onBiometricsAuthFailed() {
//        findNavController().popBackStack(R.id.configurePasscodeFragment, true)
//    }
//
//    private fun onCancel() {
//        findNavController().popBackStack(R.id.configurePasscodeFragment, true)
//    }
//
//    private fun createBiometricPrompt(
//        fragment: Fragment,
//        authCallback: BiometricPrompt.AuthenticationCallback
//    ): BiometricPrompt {
//        val executor = ContextCompat.getMainExecutor(fragment.requireContext())
//        return BiometricPrompt(fragment, executor, authCallback)
//    }
//
//    private fun createPromptInfo(context: Context, @StringRes subtitle: Int): BiometricPrompt.PromptInfo =
//        BiometricPrompt.PromptInfo.Builder().apply {
//            setTitle(context.getString(R.string.biometric_prompt_info_title))
//            setSubtitle(context.getString(subtitle))
//            setConfirmationRequired(true)
//            setNegativeButtonText(context.getString(R.string.biometric_prompt_info_cancel))
//        }.build()
}
