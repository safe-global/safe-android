package io.gnosis.safe.ui.safe.settings.safe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.gnosis.data.models.Safe
import io.gnosis.data.models.SafeInfo
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsSafeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.BaseViewBindingFragment
import io.gnosis.safe.ui.safe.settings.SettingsFragmentDirections
import io.gnosis.safe.ui.safe.settings.view.AddressItem
import io.gnosis.safe.utils.CustomAlertDialogBuilder
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import javax.inject.Inject

class SafeSettingsFragment : BaseViewBindingFragment<FragmentSettingsSafeBinding>() {

    override fun screenId() = ScreenId.SETTINGS_SAFE

    @Inject
    lateinit var viewModel: SafeSettingsViewModel

    override fun viewModelProvider() = this

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsSafeBinding =
        FragmentSettingsSafeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            remove.setOnClickListener { showRemoveDialog() }
            advanced.setOnClickListener {
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToAdvancedSafeSettingsFragment())
            }
            refresh.setOnRefreshListener { viewModel.reload() }
        }
        viewModel.state.observe(viewLifecycleOwner, Observer {
            when (val viewAction = it.viewAction) {
                is BaseStateViewModel.ViewAction.Loading -> updateUi(viewAction.isLoading, it.safe, it.safeInfo, it.ensName)
                is BaseStateViewModel.ViewAction.ShowError -> showError(viewAction.error)
            }
        })
    }

    private fun updateUi(isLoading: Boolean, safe: Safe?, safeInfo: SafeInfo?, ensNameValue: String?) {
        with(binding) {
            refresh.isRefreshing = false
            if (isLoading) {
                progress.visible(true)
                mainContainer.visible(false)
            } else {
                progress.visible(false)
                mainContainer.visible(true)
                loadSafeInfo(safe, safeInfo, ensNameValue)
            }
        }
    }

    private fun loadSafeInfo(
        safe: Safe?,
        safeInfo: SafeInfo?,
        ensNameValue: String?
    ) {
        with(binding) {
            localName.name = safe?.localName
            threshold.name = getString(R.string.safe_settings_confirmations_required, safeInfo?.threshold, safeInfo?.owners?.size)
            ownersContainer.removeAllViews()
            safeInfo?.owners?.forEach { owner -> ownersContainer.addView(ownerView(owner)) }
            masterCopy.address = safeInfo?.masterCopy
            ensName.name = ensNameValue?.takeUnless { it.isBlank() } ?: getString(R.string.safe_settings_not_set)
        }
    }

    private fun ownerView(owner: Solidity.Address): AddressItem {
        return AddressItem(requireContext()).apply {
            background = ContextCompat.getDrawable(requireContext(), R.drawable.background_selectable_white)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            address = owner
        }
    }

    private fun showError(throwable: Throwable) {
        with(binding) {
            refresh.isRefreshing = false
            mainContainer.visible(false)
            progress.visible(false)
        }
        snackbar(requireView(), throwable.message ?: getString(R.string.error_invalid_safe))
        Timber.e(throwable)
    }

    private fun showRemoveDialog() {
        CustomAlertDialogBuilder.build(
            requireContext(),
            confirmCallback = { dialog ->
                viewModel.removeSafe()
                dialog.dismiss()
            },
            contentView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_remove_safe, null),
            confirmRes = R.string.safe_settings_dialog_remove,
            confirmColor = R.color.tomato,
            cancelColor = R.color.safe_green
        ).show()
    }

    companion object {

        fun newInstance(): SafeSettingsFragment {
            return SafeSettingsFragment()
        }
    }
}
