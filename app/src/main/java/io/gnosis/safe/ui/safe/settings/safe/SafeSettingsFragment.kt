package io.gnosis.safe.ui.safe.settings.safe

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsSafeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseViewBindingFragment
import javax.inject.Inject

class SafeSettingsFragment : BaseViewBindingFragment<FragmentSettingsSafeBinding>() {

    override fun screenId() = ScreenId.SETTINGS_SAFE

    @Inject
    lateinit var viewModel: SafeSettingsViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsSafeBinding =
        FragmentSettingsSafeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.remove.setOnClickListener {
            AlertDialog.Builder(requireContext()).apply {
                setMessage(R.string.safe_settings_dialog_description)
                setNegativeButton(R.string.safe_settings_dialog_cancel) { dialog, _ -> dialog.dismiss() }
                setPositiveButton(R.string.safe_settings_dialog_remove) { _, _ -> viewModel.removeSafe() }
            }.create().show()
        }
    }

    companion object {

        fun newInstance(): SafeSettingsFragment {
            return SafeSettingsFragment()
        }
    }
}
