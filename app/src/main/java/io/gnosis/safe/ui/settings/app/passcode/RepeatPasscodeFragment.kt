package io.gnosis.safe.ui.settings.app.passcode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.data.security.HeimdallEncryptionManager
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentPasscodeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.app.SettingsHandler
import pm.gnosis.svalinn.common.utils.showKeyboardForView
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class RepeatPasscodeFragment : BaseViewBindingFragment<FragmentPasscodeBinding>() {

    override fun screenId() = ScreenId.REPEAT_PASSCODE
    private val navArgs by navArgs<RepeatPasscodeFragmentArgs>()
    private val passcodeArg by lazy { navArgs.passcode }

    @Inject
    lateinit var encryptionManager: HeimdallEncryptionManager

    @Inject
    lateinit var settingsHandler: SettingsHandler

    companion object {
        fun newInstance() = CreatePasscodeFragment()
    }

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
            createPasscode.setText(io.gnosis.safe.R.string.settings_create_passcode_repeat_the_6_digit_passcode)

            backButton.setOnClickListener {
                findNavController().popBackStack(io.gnosis.safe.R.id.repeatPasscodeFragment, true)
            }

            status.visibility = View.INVISIBLE

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
                            it.background = androidx.core.content.ContextCompat.getDrawable(requireContext(), io.gnosis.safe.R.color.surface_01)
                        }
                        (1..text.length).forEach { i ->
                            digits[i - 1].background = androidx.core.content.ContextCompat.getDrawable(
                                requireContext(),
                                io.gnosis.safe.R.drawable.ic_circle_passcode_filled_20dp
                            )
                        }
                    } else {
                        if (passcodeArg == text.toString()) {
                            encryptionManager.removePassword()
                            val success = encryptionManager.setupPassword(text.toString().toByteArray())
                            findNavController().popBackStack(io.gnosis.safe.R.id.createPasscodeFragment, true)

                            settingsHandler.usePasscode = success
                            findNavController().currentBackStackEntry?.savedStateHandle?.set(
                                io.gnosis.safe.ui.base.SafeOverviewBaseFragment.PASSCODE_SET_RESULT,
                                success
                            )

                        } else {
                            errorMessage.visible(true)
                            input.setText("")
                            digits.forEach {
                                it.background =
                                    androidx.core.content.ContextCompat.getDrawable(requireContext(), io.gnosis.safe.R.color.surface_01)
                            }
                        }
                    }
                }
            }

            skipButton.setOnClickListener {
                findNavController().popBackStack(io.gnosis.safe.R.id.createPasscodeFragment, true)
                settingsHandler.usePasscode = false
                findNavController().currentBackStackEntry?.savedStateHandle?.set(
                    io.gnosis.safe.ui.base.SafeOverviewBaseFragment.PASSCODE_SET_RESULT,
                    false
                )
            }
        }
    }
}
