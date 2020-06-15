package io.gnosis.safe.ui.safe.settings.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.safe.BuildConfig
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSettingsAppAdvancedBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseViewBindingFragment
import io.gnosis.safe.utils.formatEthAddress
import pm.gnosis.svalinn.common.utils.openUrl
import pm.gnosis.utils.asEthereumAddressString

class AdvancedAppSettingsFragment : BaseViewBindingFragment<FragmentSettingsAppAdvancedBinding>() {

    override fun screenId() = ScreenId.SETTINGS_APP_ADVANCED

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsAppAdvancedBinding =
        FragmentSettingsAppAdvancedBinding.inflate(inflater, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).setSupportActionBar(binding.advancedAppSettingsToolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        with(binding) {
            ens.blockie.setAddress(ENS_REGISTRY)
            ens.address.text = ENS_REGISTRY.formatEthAddress(requireContext(), addMiddleLinebreak = false)
            ens.root.setOnClickListener {
                requireContext().openUrl(getString(R.string.etherscan_address_url, ENS_REGISTRY.asEthereumAddressString()))
            }
            with(rpcEndpoint) {
                value = RPC_ENDPOINT
                setOnLongClickListener { copyUrlToClipboard().let { true } }
            }
            with(txService) {
                value = TX_SERVICE_ENDPOINT
                setOnLongClickListener { copyUrlToClipboard().let { true } }
            }
            with(relayService) {
                value = RELAY_SERVICE_ENDPOINT
                setOnLongClickListener { copyUrlToClipboard().let { true } }
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
        const val RELAY_SERVICE_ENDPOINT = io.gnosis.data.BuildConfig.RELAY_SERVICE_URL
    }
}
