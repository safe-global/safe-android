package io.gnosis.safe.ui.settings.safe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsSafeEditNameBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import pm.gnosis.svalinn.common.utils.hideSoftKeyboard
import pm.gnosis.svalinn.common.utils.showKeyboardForView
import javax.inject.Inject

class SafeSettingsEditNameFragment : BaseViewBindingFragment<FragmentSettingsSafeEditNameBinding>() {

    @Inject
    lateinit var viewModel: SafeSettingsEditNameViewModel

    override fun screenId() = ScreenId.SETTINGS_SAFE_EDIT_NAME

    override fun viewModelProvider() = this

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsSafeEditNameBinding =
        FragmentSettingsSafeEditNameBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            backButton.setOnClickListener {
                close()
            }
            safeName.setOnEditorActionListener listener@ { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    viewModel.saveLocalName(safeName.text.toString())
                    close()
                    return@listener true
                }
                return@listener false
            }
            safeName.showKeyboardForView()
        }

        viewModel.state.observe(viewLifecycleOwner, Observer {
            binding.safeName.setText(it.name)
            binding.safeName.setSelection(it.name?.length ?: 0)
        })
    }

    private fun close() {
        activity?.hideSoftKeyboard()
        findNavController().navigateUp()
    }
}
