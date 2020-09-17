package io.gnosis.safe.ui.settings.safe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import io.gnosis.data.models.SafeInfo
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsSafeAdvancedBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.view.NamedAddressItem
import io.gnosis.safe.ui.settings.view.SettingItem
import io.gnosis.safe.utils.dpToPx
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

class AdvancedSafeSettingsFragment : BaseViewBindingFragment<FragmentSettingsSafeAdvancedBinding>() {

    @Inject
    lateinit var viewModel: AdvancedSafeSettingsViewModel

    override fun screenId(): ScreenId? = ScreenId.SETTINGS_SAFE_ADVANCED

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsSafeAdvancedBinding =
        FragmentSettingsSafeAdvancedBinding.inflate(inflater, container, false)

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
         viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (val viewAction = state.viewAction) {
                is LoadSafeInfo -> setUi(state.isLoading, viewAction.safeInfo)
                is BaseStateViewModel.ViewAction.ShowError -> handleError(viewAction.error)
                else -> setUi(state.isLoading)
            }
        })
        with(binding) {
            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
            }
            refresh.setOnRefreshListener {
                viewModel.load()
            }
        }
        viewModel.load()
    }

    private fun setUi(isLoading: Boolean, safeInfo: SafeInfo? = null) {
        updateLoading(isLoading)
        safeInfo?.let { setSafeInfo(it) }
    }

    private fun setSafeInfo(safeInfo: SafeInfo) {
        with(binding) {
            fallbackHandlerContainer.removeAllViews()
            fallbackHandlerContainer.addView(fallbackHandlerView(safeInfo.fallbackHandler))
            nonce.name = safeInfo.nonce.toString()
            modulesContainer.removeAllViews()
            safeInfo.modules.takeUnless { it.isEmpty() }?.let {
                moduleLabel.visible(true)
                it.forEach { module ->
                    modulesContainer.addView(labeledAddress(module))
                }
            } ?: run { moduleLabel.visible(false) }
        }
    }

    private fun fallbackHandlerView(fallbackHandler: Solidity.Address?): View =
        when {
            fallbackHandler == null || fallbackHandler.value == BigInteger.ZERO -> SettingItem(requireContext()).apply {
                background = ContextCompat.getDrawable(requireContext(), R.drawable.background_selectable_white)
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dpToPx(60))
                openable = false
                name = getString(R.string.safe_settings_not_set)
            }
            viewModel.isDefaultFallbackHandler(fallbackHandler) -> labeledAddress(fallbackHandler, R.string.safe_settings_default_fallback_handler)
            else -> labeledAddress(fallbackHandler, R.string.safe_settings_unknown)
        }

    private fun labeledAddress(address: Solidity.Address, @StringRes labelId: Int): NamedAddressItem =
        labeledAddress(address, getString(labelId))

    private fun labeledAddress(address: Solidity.Address, label: String = ""): NamedAddressItem {
        return NamedAddressItem(requireContext()).apply {
            background = ContextCompat.getDrawable(requireContext(), R.drawable.background_selectable_white)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            this.address = address
            this.label = label
        }
    }

    private fun handleError(throwable: Throwable) {
        with(binding) {
            refresh.isRefreshing = false
            mainContainer.visible(false)
            progress.visible(false)
        }
        snackbar(requireView(), throwable.message ?: getString(R.string.error_invalid_safe))
        Timber.e(throwable)
    }

    private fun updateLoading(showLoading: Boolean) {
        with(binding) {
            refresh.isRefreshing = false
            mainContainer.visible(!showLoading)
            progress.visible(showLoading)
        }
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
