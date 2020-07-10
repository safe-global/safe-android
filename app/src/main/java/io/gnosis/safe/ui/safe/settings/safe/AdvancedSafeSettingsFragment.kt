package io.gnosis.safe.ui.safe.settings.safe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentAdvancedSafeSettingsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseViewBindingFragment
import javax.inject.Inject

class AdvancedSafeSettingsFragment : BaseViewBindingFragment<FragmentAdvancedSafeSettingsBinding>() {

    @Inject
    lateinit var viewModel: AdvancedSafeSettingsViewModel

    override fun screenId(): ScreenId? = ScreenId.SETTINGS_SAFE_ADVANCED

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAdvancedSafeSettingsBinding =
        FragmentAdvancedSafeSettingsBinding.inflate(inflater, container, false)

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        (activity as AppCompatActivity).setSupportActionBar(binding.advancedAppSettingsToolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewModel.load()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
