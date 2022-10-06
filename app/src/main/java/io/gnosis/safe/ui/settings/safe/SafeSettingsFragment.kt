package io.gnosis.safe.ui.settings.safe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.gnosis.data.models.*
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsSafeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.errorSnackbar
import io.gnosis.safe.toError
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.*
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.SettingsFragmentDirections
import io.gnosis.safe.ui.settings.view.AddressItem
import io.gnosis.safe.ui.settings.view.NamedAddressItem
import io.gnosis.safe.utils.shortChecksumString
import io.gnosis.safe.utils.showConfirmDialog
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
            remove.setOnClickListener {
                showConfirmDialog(context = requireContext(), message = R.string.safe_settings_dialog_description) {
                    viewModel.removeSafe()
                }
            }
            advanced.setOnClickListener {
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToAdvancedSafeSettingsFragment())
            }
            refresh.setOnRefreshListener { viewModel.reload() }
        }

        viewModel.state.observe(viewLifecycleOwner, Observer {
            binding.contentNoData.root.visible(false)
            when (val viewAction = it.viewAction) {
                is Loading -> {
                    didLoadOnce = if (didLoadOnce) didLoadOnce else !viewAction.isLoading
                    binding.progress.visible(!didLoadOnce && viewAction.isLoading)
                    binding.refresh.isRefreshing = didLoadOnce && viewAction.isLoading
                    if (!viewAction.isLoading)
                        showSafeInfo(it.safe, it.safeInfo, it.ensName, it.localOwners)
                }
                is UpdateActiveSafe -> {
                    hideContent()
                    didLoadOnce = false
                }
                is ShowError -> {
                    hideLoading()
                    if (!didLoadOnce) {
                        hideContent()
                        showContentNoData()
                    }
                    val error = viewAction.error.toError()
                    if (error.trackingRequired) {
                        tracker.logException(viewAction.error)
                    }
                    errorSnackbar(requireView(), error.message(requireContext(), R.string.error_description_safe_settings))
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (!viewModel.isLoading()) {
            viewModel.reload()
        }
    }

    private fun showContentNoData() {
        binding.contentNoData.root.visible(true)
    }

    private fun hideContent() {
        binding.mainContainer.visible(false)
    }

    private fun showSafeInfo(
        safe: Safe?,
        safeInfo: SafeInfo?,
        ensNameValue: String?,
        localOwners: List<Owner>
    ) {
        with(binding) {
            localName.name = safe?.localName
            threshold.name = getString(R.string.safe_settings_confirmations_required, safeInfo?.threshold, safeInfo?.owners?.size)
            ownersContainer.removeAllViews()
            safeInfo?.owners?.forEach() { owner ->
                ownersContainer.addView(ownerView(safe!!.chain, owner, localOwners, owner !== safeInfo.owners.last()))
            }
            masterCopy.setAddress(safe?.chain, safeInfo?.implementation?.value, safeInfo?.version)
            masterCopy.loadKnownAddressLogo(safeInfo?.implementation?.logoUri, safeInfo?.implementation?.value)
            ensName.name = ensNameValue?.takeUnless { it.isBlank() } ?: getString(R.string.safe_settings_not_set_reverse_record)
            mainContainer.visible(true)
        }
    }

    private fun ownerView(chain: Chain, owner: AddressInfo, localOwners: List<Owner>, showTrailingSeparator: Boolean): View {

        val localOwner = localOwners.find { it.address == owner.value }

        return if (localOwner != null) {
            // use local owner name & identicon
            NamedAddressItem(requireContext()).apply {
                background = ContextCompat.getDrawable(requireContext(), R.drawable.background_secondary_selectable)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, resources.getDimension(R.dimen.item_address).toInt())
                setAddress(chain, owner.value, localOwner.type)
                name = if (localOwner.name.isNullOrBlank())
                    context.getString(
                        R.string.settings_app_imported_owner_key_default_name,
                        localOwner.address.shortChecksumString()
                    ) else localOwner.name
                showSeparator = showTrailingSeparator
            }
        } else {
            if (!owner.name.isNullOrBlank()) {
                // use remote owner name & logo if available
                NamedAddressItem(requireContext()).apply {
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.background_secondary_selectable)
                    layoutParams =
                        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, resources.getDimension(R.dimen.item_address).toInt())
                    setAddress(chain, owner.value)
                    name = owner.name
                    showSeparator = showTrailingSeparator
                    loadKnownAddressLogo(owner.logoUri, owner.value)
                }
            } else {
                AddressItem(requireContext()).apply {
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.background_secondary_selectable)
                    layoutParams =
                        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, resources.getDimension(R.dimen.item_address).toInt())
                    setAddress(chain, owner.value)
                    showSeparator = showTrailingSeparator
                }
            }
        }
    }

    private fun hideLoading() {
        with(binding) {
            refresh.isRefreshing = false
            progress.visible(false)
        }
    }

    companion object {

        fun newInstance(): SafeSettingsFragment {
            return SafeSettingsFragment()
        }
    }
}
