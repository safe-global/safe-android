package io.gnosis.safe.ui.settings.app.passcode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.data.security.HeimdallEncryptionManager
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsCreatePasscodeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment.Companion.PASSCODE_SET_RESULT
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import pm.gnosis.svalinn.common.utils.showKeyboardForView
import timber.log.Timber
import javax.inject.Inject

class CreatePasscodeFragment : BaseViewBindingFragment<FragmentSettingsCreatePasscodeBinding>() {

    override fun screenId() = ScreenId.CREATE_PASSCODE
    private val navArgs by navArgs<CreatePasscodeFragmentArgs>()

    @Inject
    lateinit var encryptionManagerX: HeimdallEncryptionManager

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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.i("----> passcodeArg: $passcodeArg")


        //TODO Open numeric keyboard
        //Reopen it when one of the circles is touched

        with(binding) {
            if (passcodeArg?.isNotEmpty() == true) {
                createPasscode.setText(R.string.settings_create_passcode_repeat_the_6_digit_passcode)
            }
            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
            }

            val digits = listOf(digit1, digit2, digit3, digit4, digit5, digit6)
            input.showKeyboardForView()
            Timber.i("---> setOnEditorActionListener()")

            input.setOnEditorActionListener { v, actionId, event ->
                actionId == EditorInfo.IME_ACTION_DONE
            }

            input.doOnTextChanged { text, _, _, _ ->
                Timber.i("--->       text:  $text")

                text?.let {

                    if (input.text.length < 6) {
                        digits.forEach {
                            it.background = ContextCompat.getDrawable(requireContext(), R.color.surface_01)
                        }
                        (1..text.length).forEach { i ->
                            digits[i - 1].background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_circle_passcode_filled_20dp)
                        }
                    } else {

                        // 6 digits have been entered

                        if (passcodeArg != null) {
                            // we're on the confirm screen and need to verify both passcodes are the same
                            // TODO: Compare passcode text == passcode
                            if (passcodeArg == text.toString()) {

                                Timber.i("---> Passcode matches")

                                // pass codes are the same an need to be stored

                                //TODO: Store passcode
                               val success =  encryptionManagerX.setupPassword(text.toString().toByteArray())
                                //TODO: Navigate back and show success snackbar

                                Timber.i("---> Passcode saved: $success")
                                // pop confirm fragment
                                val popped = findNavController().popBackStack(R.id.createPasscodeFragment, true)

                                // pop first passcode fragment
                                if (popped) findNavController().popBackStack(R.id.createPasscodeFragment, true)
                                // pass on success of setting pass code


                                findNavController().currentBackStackEntry?.savedStateHandle?.set(PASSCODE_SET_RESULT, success)

                            } else {
                                // TODO("Show error")

                                Timber.w("---> Passcode did not match")
                            }
                        } else {

                            Timber.i("---> Passcode entered once")

                            //TODO: Navigate to confirm screen
                            findNavController().navigate(
                                CreatePasscodeFragmentDirections.actionCreatePasscodeFragmentToCreatePasscodeFragment(
                                    passcode = text.toString()
                                )
                            )
                        }
                    }
                }
            }
            skipButton.setOnClickListener {

                Timber.i("---> Skip flow.")
                val popped = findNavController().popBackStack(R.id.createPasscodeFragment, true)
                if (popped) findNavController().popBackStack(R.id.createPasscodeFragment, true)

                findNavController().currentBackStackEntry?.savedStateHandle?.set(PASSCODE_SET_RESULT, false)
            }

        }
    }
}
