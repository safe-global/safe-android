package io.gnosis.safe.ui.settings.app.passcode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.data.security.HeimdallEncryptionManager
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsCreatePasscodeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.app.SettingsHandler
import pm.gnosis.svalinn.common.utils.showKeyboardForView
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import javax.inject.Inject

class RepeatPasscodeFragment : BaseViewBindingFragment<FragmentSettingsCreatePasscodeBinding>() {

    override fun screenId() = ScreenId.REPEAT_PASSCODE
    private val navArgs by navArgs<RepeatPasscodeFragmentArgs>()

    @Inject
    lateinit var encryptionManager: HeimdallEncryptionManager

    @Inject
    lateinit var settingsHandler: SettingsHandler

    private val passcodeArg by lazy { navArgs.passcode }

    companion object {
        fun newInstance() = CreatePasscodeFragment()
    }

//    private lateinit var viewModel: CreatePasscodeViewModel

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsCreatePasscodeBinding =
        FragmentSettingsCreatePasscodeBinding.inflate(inflater, container, false)

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun onResume() {
        super.onResume()
        binding.input.showKeyboardForView()
        Timber.i("---> onResume() passcodeArg: $passcodeArg")
        Timber.i("---> onResume()  input.text: ${binding.input.text}")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            Timber.i("---> createPasscode(repeat_the_6_digit_passcode)")
            createPasscode.setText(io.gnosis.safe.R.string.settings_create_passcode_repeat_the_6_digit_passcode)

            backButton.setOnClickListener {
                findNavController().popBackStack(io.gnosis.safe.R.id.repeatPasscodeFragment, true)
            }

            status.visibility = View.INVISIBLE

            val digits = kotlin.collections.listOf(digit1, digit2, digit3, digit4, digit5, digit6)
            input.showKeyboardForView()
            Timber.i("---> setOnEditorActionListener()")

            input.setOnEditorActionListener { v, actionId, event ->
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE
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

                            Timber.d("---> Passcode matches")

                            encryptionManager.removePassword()
                            val success = encryptionManager.setupPassword(text.toString().toByteArray())
                            val popped = findNavController().popBackStack(io.gnosis.safe.R.id.createPasscodeFragment, true)
                            if (popped) findNavController().popBackStack(io.gnosis.safe.R.id.createPasscodeFragment, true)

                            settingsHandler.usePasscode = success
                            findNavController().currentBackStackEntry?.savedStateHandle?.set(
                                io.gnosis.safe.ui.base.SafeOverviewBaseFragment.PASSCODE_SET_RESULT,
                                success
                            )

                        } else {
                            Timber.d("---> Passcode did not match")
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
                Timber.i("---> Skip flow.")
                val popped = findNavController().popBackStack(io.gnosis.safe.R.id.createPasscodeFragment, true)
                if (popped) findNavController().popBackStack(io.gnosis.safe.R.id.createPasscodeFragment, true)
                settingsHandler.usePasscode = false
                findNavController().currentBackStackEntry?.savedStateHandle?.set(
                    io.gnosis.safe.ui.base.SafeOverviewBaseFragment.PASSCODE_SET_RESULT,
                    false
                )
            }
        }
    }
}

