package io.gnosis.safe.ui.settings.app.passcode

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
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
import io.gnosis.safe.ui.settings.app.SettingsHandler
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
                    snackbar(requireView(), R.string.passcode_disabled)
                    if (requirePasscodeToOpen) {
                        findNavController().popBackStack(R.id.enterPasscodeFragment, true)
                    } else {
                        findNavController().popBackStack(R.id.transactionDetailsFragment, false)
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
                    if (requirePasscodeToOpen) {
                        findNavController().popBackStack(R.id.enterPasscodeFragment, true)
                        binding.input.hideSoftKeyboard()
                    } else {
                        findNavController().popBackStack(R.id.signingOwnerSelectionFragment, true)
                        findNavController().currentBackStackEntry?.savedStateHandle?.set(
                            SafeOverviewBaseFragment.OWNER_SELECTED_RESULT,
                            selectedOwner
                        )
                    }
                }
            }
        })

        with(binding) {
            title.setText(R.string.settings_passcode_enter_passcode)
            createPasscode.setText(R.string.settings_passcode_enter_your_current_passcode)
            helpText.visible(false)
            fingerprint.visible(settingsHandler.useBiometrics)

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
}
