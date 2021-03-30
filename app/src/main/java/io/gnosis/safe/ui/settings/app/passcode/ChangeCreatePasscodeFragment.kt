package io.gnosis.safe.ui.settings.app.passcode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doOnTextChanged
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

class ChangeCreatePasscodeFragment : BaseViewBindingFragment<FragmentPasscodeBinding>() {

    override fun screenId() = ScreenId.PASSCODE_CHANGE_CREATE
    private val navArgs by navArgs<ChangeCreatePasscodeFragmentArgs>()
    private val oldPasscode by lazy { navArgs.oldPasscode }

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
            title.setText(R.string.settings_passcode_change_passcode)

            status.visibility = View.INVISIBLE

            backButton.setOnClickListener {
                skipPasscodeSetup()
            }
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    skipPasscodeSetup()
                }
            })

            val digits = listOf(digit1, digit2, digit3, digit4, digit5, digit6)
            input.showKeyboardForView()

            //Disable done button
            input.setOnEditorActionListener { _, actionId, _ ->
                actionId == EditorInfo.IME_ACTION_DONE
            }

            input.doOnTextChanged(onSixDigitsHandler(digits, requireContext()) { digitsAsString ->
                input.setText("") // So it is empty, when the user navigates back
                findNavController().navigate(
                    ChangeCreatePasscodeFragmentDirections.actionPasscodeSettingsFragmentToChangeRepeatPasscodeFragment(
                        passcode = digitsAsString,
                        oldPasscode = oldPasscode
                    )
                )
            })

            // Skip Button
            actionButton.visible(false)
        }
    }

    private fun skipPasscodeSetup() {
        findNavController().popBackStack(R.id.changePasscodeFragment, true)
    }
}
