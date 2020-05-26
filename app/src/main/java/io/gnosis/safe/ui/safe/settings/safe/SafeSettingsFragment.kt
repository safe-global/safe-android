package io.gnosis.safe.ui.safe.settings.safe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsSafeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment
import javax.inject.Inject

class SafeSettingsFragment : BaseFragment<FragmentSettingsSafeBinding>() {

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
        binding.button.setOnClickListener {
            viewModel.removeSafe()
        }
    }

    companion object {

        fun newInstance(): SafeSettingsFragment {
            return SafeSettingsFragment()
        }
    }
}
