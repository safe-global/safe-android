package io.gnosis.safe.ui.settings.app

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.safe.BuildConfig
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsAppAdvancedBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment

class AdvancedAppSettingsFragment : BaseViewBindingFragment<FragmentSettingsAppAdvancedBinding>() {

    override fun screenId() = ScreenId.SETTINGS_APP_ADVANCED

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsAppAdvancedBinding =
        FragmentSettingsAppAdvancedBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            ens.address = ENS_REGISTRY
            with(rpcEndpoint) {
                value = RPC_ENDPOINT
                setOnLongClickListener { copyUrlToClipboard().let { true } }
            }
            with(txService) {
                value = Uri.parse(TX_SERVICE_ENDPOINT).let {
                    Uri.Builder()
                        .scheme(it.scheme)
                        .encodedAuthority(it.authority)
                        .build()
                        .toString()
                }
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
            }
            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
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
        const val TX_SERVICE_ENDPOINT = io.gnosis.data.BuildConfig.TRANSACTION_SERVICE_URL
        const val CLIENT_GATEWAY_SERVICE_ENDPOINT = io.gnosis.data.BuildConfig.CLIENT_GATEWAY_URL
    }
}
