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
import androidx.navigation.fragment.navArgs
import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.Chain
import io.gnosis.data.models.SafeInfo
import io.gnosis.data.utils.SemVer
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsSafeAdvancedBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.errorSnackbar
import io.gnosis.safe.toError
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.ShowError
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.safe.send_funds.AssetSelectionFragmentArgs
import io.gnosis.safe.ui.settings.view.NamedAddressItem
import io.gnosis.safe.ui.settings.view.SettingItem
import io.gnosis.safe.utils.appendLink
import io.gnosis.safe.utils.dpToPx
import io.gnosis.safe.utils.toColor
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

class AdvancedSafeSettingsFragment : BaseViewBindingFragment<FragmentSettingsSafeAdvancedBinding>() {

    private val navArgs by navArgs<AssetSelectionFragmentArgs>()
    private val chain by lazy { navArgs.chain }

    override fun screenId() = ScreenId.SETTINGS_SAFE_ADVANCED

    override suspend fun chainId() = chain.chainId

    @Inject
    lateinit var viewModel: AdvancedSafeSettingsViewModel

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsSafeAdvancedBinding =
        FragmentSettingsSafeAdvancedBinding.inflate(inflater, container, false)

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (val viewAction = state.viewAction) {
                is LoadSafeInfo -> setUi(state.isLoading, viewAction.chain, viewAction.safeInfo)
                is ShowError -> handleError(viewAction)
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
            chainRibbon.text = chain.name
            chainRibbon.setTextColor(
                chain.textColor.toColor(
                    requireContext(),
                    R.color.white
                )
            )
            chainRibbon.setBackgroundColor(
                chain.backgroundColor.toColor(
                    requireContext(),
                    R.color.primary
                )
            )
        }
        viewModel.load()
    }

    private fun handleError(viewAction: ShowError) {
        with(binding) {
            refresh.isRefreshing = false
            mainContainer.visible(false)
            progress.visible(false)
        }
        val error = viewAction.error.toError()
        if (error.trackingRequired) {
            tracker.logException(viewAction.error)
        }
        errorSnackbar(requireView(), error.message(requireContext(), R.string.error_description_safe_settings_advanced))
        Timber.e(viewAction.error)
    }

    private fun setUi(isLoading: Boolean, chain: Chain? = null, safeInfo: SafeInfo? = null) {
        updateLoading(isLoading)
        safeInfo?.let { setSafeInfo(chain!!, it) }
    }

    private fun setSafeInfo(chain: Chain, safeInfo: SafeInfo) {
        with(binding) {

            fallbackHandlerContainer.removeAllViews()
            fallbackHandlerContainer.addView(safeParameterView(chain, safeInfo.fallbackHandler, R.string.unknown_fallback_handler))
            fallbackHandlerHelpLink.text = ""
            fallbackHandlerHelpLink.appendLink(
                urlText = resources.getString(R.string.safe_settings_fallback_handler_help),
                url = resources.getString(R.string.safe_settings_fallback_handler_help_url),
                linkIcon = R.drawable.ic_external_link_green_16dp
            )

            nonce.name = safeInfo.nonce.toString()
            modulesContainer.removeAllViews()
            safeInfo.modules.takeUnless { it.isNullOrEmpty() }?.let {
                moduleLabel.visible(true)
                it.forEach { module ->
                    modulesContainer.addView(labeledAddress(chain, module.value, module.name ?: getString(R.string.unknown_module), module.logoUri))
                }
            } ?: run { moduleLabel.visible(false) }

            val contractVersion = SemVer.parse(safeInfo.version)
            if (contractVersion >= SemVer(1, 3, 0)) {

                guardContainer.removeAllViews()
                guardContainer.addView(safeParameterView(chain, safeInfo.guard, R.string.unknown_guard))
                guardHelpLink.text = ""
                guardHelpLink.appendLink(
                    urlText = resources.getString(R.string.safe_settings_guard_help),
                    url = resources.getString(R.string.safe_settings_guard_help_url),
                    linkIcon = R.drawable.ic_external_link_green_16dp
                )

                guardHeader.visible(true)
                guardContainer.visible(true)
                guardHelpLink.visible(true)

            } else {
                guardHeader.visible(false)
                guardContainer.visible(false)
                guardHelpLink.visible(false)
            }
        }
    }


    private fun safeParameterView(chain: Chain, addressInfo: AddressInfo?, @StringRes unknownString: Int): View  =
        when {
            addressInfo == null || addressInfo.value.value == BigInteger.ZERO -> SettingItem(requireContext()).apply {
                background = ContextCompat.getDrawable(requireContext(), R.drawable.background_secondary_selectable)
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dpToPx(60))
                openable = false
                name = getString(R.string.safe_settings_not_set)
            }
            else -> labeledAddress(chain, addressInfo.value, addressInfo.name ?: getString(unknownString), addressInfo.logoUri)
        }

    private fun labeledAddress(chain: Chain, address: Solidity.Address, label: String, logoUri: String? = null): NamedAddressItem {
        return NamedAddressItem(requireContext()).apply {
            background = ContextCompat.getDrawable(requireContext(), R.drawable.background_secondary_selectable)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setAddress(chain, address)
            this.name = label
            logoUri?.let { this.loadKnownAddressLogo(it, address) }
            showSeparator = true
        }
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
