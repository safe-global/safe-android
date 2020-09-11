package io.gnosis.safe.ui.settings.safe

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
import io.gnosis.safe.helpers.Offline
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.Loading
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.ShowError
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.SettingsFragmentDirections
import io.gnosis.safe.ui.settings.view.AddressItem
import io.gnosis.safe.utils.CustomAlertDialogBuilder
import io.gnosis.safe.utils.getErrorResForException
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class SafeSettingsFragment : BaseViewBindingFragment<FragmentSettingsSafeBinding>() {

    override fun screenId() = ScreenId.SETTINGS_SAFE

    private var didLoadOnce = false

    @Inject
    lateinit var viewModel: SafeSettingsViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsSafeBinding =
        FragmentSettingsSafeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            localName.setOnClickListener {
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToSafeSettingsEditNameFragment())
            }
            remove.setOnClickListener { showRemoveDialog() }
            advanced.setOnClickListener {
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToAdvancedSafeSettingsFragment())
            }
            refresh.setOnRefreshListener { viewModel.reload() }
        }
        viewModel.state.observe(viewLifecycleOwner, Observer {
            when (val viewAction = it.viewAction) {
                is Loading -> {
                    didLoadOnce = if (didLoadOnce) didLoadOnce else !viewAction.isLoading
                    updateUi(viewAction.isLoading, it.safe, it.safeInfo, it.ensName)
                }
                is ShowError -> {
                    if (!didLoadOnce) {
                        showContentNoData()
                    }
                    hideLoading()
                    when (viewAction.error) {
                        is Offline -> {
                            snackbar(requireView(), R.string.error_no_internet)
                        }
                        else -> {
                            snackbar(requireView(), viewAction.error.getErrorResForException())
                        }
                    }
                }
            }
        })
    }

    private fun showContentNoData() {
        binding.contentNoData.root.visible(true)
        binding.mainContainer.visibility = View.GONE
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
            masterCopy.setAddress(safeInfo?.masterCopy)
            ensName.name = ensNameValue?.takeUnless { it.isBlank() } ?: getString(R.string.safe_settings_not_set_reverse_record)
        }
    }

    private fun ownerView(owner: Solidity.Address): AddressItem {
        return AddressItem(requireContext()).apply {
            background = ContextCompat.getDrawable(requireContext(), R.drawable.background_selectable_white)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            address = owner
            showSeparator = true
        }
    }

    private fun hideLoading() {
        with(binding) {
            refresh.isRefreshing = false
            progress.visible(false)
        }
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
