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
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsCreatePasscodeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment.Companion.OWNER_IMPORT_RESULT
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import pm.gnosis.svalinn.common.utils.showKeyboardForView
import timber.log.Timber

class CreatePasscodeFragment : BaseViewBindingFragment<FragmentSettingsCreatePasscodeBinding>() {

    override fun screenId() = ScreenId.CREATE_PASSCODE
    private val navArgs by navArgs<CreatePasscodeFragmentArgs>()

    private val passcode by lazy { navArgs.passcode }

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

        Timber.i("----> passcode: $passcode")
        //TODO Open numeric keyboard
        //Reopen it when one of the circles is touched

        with(binding) {
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
                Timber.i("---> ${input.text}")
                Timber.i("---> $text")

                text?.let {

                    if (input.text.length < 6) {
                        digits.forEach {
                            it.background = ContextCompat.getDrawable(requireContext(), R.color.surface_01)
                        }
                        (1..text.length).forEach { i ->
                            digits[i - 1].background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_circle_passcode_filled_20dp)
                        }
                    } else {
                        findNavController().navigate(CreatePasscodeFragmentDirections.actionCreatePasscodeFragmentToCreatePasscodeFragment())
                        if (passcode != null) {
                            findNavController().popBackStack(R.id.createPasscodeFragment, true)
                        }
                    }
                }
            }
            skipButton.setOnClickListener {
                findNavController().popBackStack(R.id.createPasscodeFragment, true)
                findNavController().currentBackStackEntry?.savedStateHandle?.set(OWNER_IMPORT_RESULT, true)
            }

        }
    }
}
