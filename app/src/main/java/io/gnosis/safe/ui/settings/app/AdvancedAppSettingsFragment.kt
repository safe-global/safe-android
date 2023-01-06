package io.gnosis.safe.ui.settings.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import io.gnosis.data.BuildConfig.CLIENT_GATEWAY_URL
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.safe.BuildConfig
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsAppAdvancedBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.appendLink
import pm.gnosis.svalinn.common.utils.visible
import java.math.BigInteger
import javax.inject.Inject

class AdvancedAppSettingsFragment : BaseViewBindingFragment<FragmentSettingsAppAdvancedBinding>() {

    override fun screenId() = ScreenId.SETTINGS_APP_ADVANCED

    override suspend fun chainId(): BigInteger? = null

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    @Inject
    lateinit var settingsHandler: SettingsHandler

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsAppAdvancedBinding =
        FragmentSettingsAppAdvancedBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            //FIXME: reuse commented code when implementing network details screen; then clean up
            /*ens.address = ENS_REGISTRY
            with(rpcEndpoint) {
                value = RPC_ENDPOINT
                setOnLongClickListener { copyUrlToClipboard().let { true } }
            }
            with(clientGatewayService) {
                value = Uri.parse(CLIENT_GATEWAY_SERVICE_ENDPOINT).let {
                    Uri.Builder()
                        .scheme(it.scheme)
                        .encodedAuthority(it.authority)
                        .path("about")
                        .build()
                        .toString()
                }
                setOnLongClickListener { copyUrlToClipboard().let { true } }
            }*/
            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
            }
            screenshotPermission.settingSwitch.isChecked = settingsHandler.screenshotsAllowed
            screenshotPermission.settingSwitch.setOnClickListener {
                settingsHandler.screenshotsAllowed = screenshotPermission.settingSwitch.isChecked
                activity?.window?.let { window ->
                    settingsHandler.allowScreenShots(window, screenshotPermission.settingSwitch.isChecked)
                }
            }
            trackingHelpText.appendLink(
                url = getString(R.string.link_privacy_policy),
                urlText = getString(R.string.tracking_text),
                underline = true,
                linkIcon = R.drawable.ic_external_link_green_16dp
            )
            trackingPermission.settingSwitch.isChecked = settingsHandler.trackingAllowed
            trackingPermission.settingSwitch.setOnClickListener {
                settingsHandler.trackingAllowed = trackingPermission.settingSwitch.isChecked
                settingsHandler.allowTracking(requireContext(), trackingPermission.settingSwitch.isChecked)
            }

            if (CLIENT_GATEWAY_URL.contains("staging")) {
                debugContainer.visible(true)
                crashTheApp.setOnClickListener {
                    throw RuntimeException("Deliberate Crash")
                }
            } else {
                debugContainer.visible(false)
            }
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

    companion object {

        val ENS_REGISTRY = EnsRepository.ENS_ADDRESS

        // TODO: Add constants to respective repositories
        const val RPC_ENDPOINT = BuildConfig.BLOCKCHAIN_NET_URL
        const val CLIENT_GATEWAY_SERVICE_ENDPOINT = io.gnosis.data.BuildConfig.CLIENT_GATEWAY_URL
    }
}
