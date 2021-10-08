package io.gnosis.safe.ui.settings.app.passcode

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentPasscodeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.app.SettingsHandler
import pm.gnosis.svalinn.common.utils.showKeyboardForView
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class CreatePasscodeFragment : BaseViewBindingFragment<FragmentPasscodeBinding>() {

    override fun screenId() = ScreenId.PASSCODE_CREATE
    private val navArgs by navArgs<CreatePasscodeFragmentArgs>()
    private val ownerImported by lazy { navArgs.ownerImported }
    private val ownerAddress by lazy { navArgs.ownerAddress }

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

        with(binding) {

            status.visible(ownerImported, View.INVISIBLE)

            backButton.setOnClickListener {
                skipPasscodeSetup()
            }

            val digits = listOf(digit1, digit2, digit3, digit4, digit5, digit6)
            input.showKeyboardForView()

            //Disable done button
            input.setOnEditorActionListener { _, actionId, _ ->
                actionId == EditorInfo.IME_ACTION_DONE
            }

            input.doOnTextChanged(onSixDigitsHandler(digits, requireContext()) { digitsAsString ->
                findNavController().navigate(
                    CreatePasscodeFragmentDirections.actionCreatePasscodeFragmentToRepeatPasscodeFragment(
                        passcode = digitsAsString,
                        ownerImported = ownerImported
                    )
                )
                input.setText("") // So it is empty, when the user navigates back
            })

            // Skip Button
            actionButton.setOnClickListener {
                skipPasscodeSetup()
            }
            rootView.setOnClickListener {
                input.showKeyboardForView()
            }
        }
    }

    private fun FragmentPasscodeBinding.skipPasscodeSetup() {
        input.hideSoftKeyboard()

        settingsHandler.usePasscode = false
        tracker.setPasscodeIsSet(false)
        tracker.logPasscodeSkipped()

        if (ownerImported) {
            findNavController().popBackStack(R.id.ownerAddOptionsFragment, true)
            findNavController().navigate(R.id.action_to_owner_details, Bundle().apply {
                putString("ownerAddress", ownerAddress!!)
            })
        } else {
            findNavController().popBackStack(R.id.createPasscodeFragment, true)
        }
        findNavController().currentBackStackEntry?.savedStateHandle?.set(SafeOverviewBaseFragment.OWNER_IMPORT_RESULT, false)
        findNavController().currentBackStackEntry?.savedStateHandle?.set(SafeOverviewBaseFragment.PASSCODE_SET_RESULT, false)
    }
}
