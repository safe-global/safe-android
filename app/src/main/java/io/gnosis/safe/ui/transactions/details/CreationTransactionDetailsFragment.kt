package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionDetailsCreationBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.transactions.details.view.TxType
import pm.gnosis.svalinn.common.utils.copyToClipboard
import pm.gnosis.svalinn.common.utils.openUrl
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress

class CreationTransactionDetailsFragment : BaseViewBindingFragment<FragmentTransactionDetailsCreationBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS_DETAILS

    private val navArgs by navArgs<CreationTransactionDetailsFragmentArgs>()

    private val statusTextRes by lazy { navArgs.statusTextRes }
    private val statusColorRes by lazy { navArgs.statusColorRes }
    private val transActionHash by lazy { navArgs.transActionHash }
    private val creator by lazy { navArgs.creator }
    private val creatorName by lazy { navArgs.creatorName }
    private val creatorLogoUri by lazy { navArgs.creatorLogoUri }
    private val implementation by lazy { navArgs.implementation }
    private val implementationName by lazy { navArgs.implementationName }
    private val implementationLogoUri by lazy { navArgs.implementationLogoUri }
    private val factory by lazy { navArgs.factory }
    private val factoryName by lazy { navArgs.factoryName }
    private val factoryLogoUri by lazy { navArgs.factoryLogoUri }
    private val dateTimeText by lazy { navArgs.dateTimeText }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTransactionDetailsCreationBinding =
        FragmentTransactionDetailsCreationBinding.inflate(inflater, container, false)

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
            }
            statusItem.setStatus(
                titleRes = TxType.CREATION.titleRes,
                iconRes = TxType.CREATION.iconRes,
                statusTextRes = statusTextRes,
                statusColorRes = statusColorRes
            )
            txHashItem.label = getString(R.string.tx_details_advanced_hash)
            txHashItem.value = transActionHash
            txHashItem.setOnClickListener {
                context?.copyToClipboard(context?.getString(R.string.hash_copied)!!, txHashItem.value.toString()) {
                    snackbar(view = root, textId = R.string.copied_success)
                }
            }

            creatorItemTitle.text = getString(R.string.tx_details_creation_creator_address)
            with(creatorItem) {
                val creatorAddress = creator!!.asEthereumAddress()
                address = creatorAddress
                name = creatorName ?: getString(R.string.unknown_creator)
                loadKnownAddressLogo(creatorLogoUri, creatorAddress)
            }

            implementationTitle.text = getString(R.string.tx_details_creation_implementation_used)
            if (implementation != null) {
                implementationItem.setAddress(implementation!!.asEthereumAddress(), implementationName, false)
                noImplementationItem.visible(false)
            } else {
                noImplementationItem.text = getString(R.string.tx_details_creation_no_implementation_available)
                noImplementationItem.visible(true)
            }

            factoryTitle.text = getString(R.string.tx_details_creation_factory_used)
            if (factory != null) {
                with(factoryItem) {
                    val factoryAddress = factory!!.asEthereumAddress()
                    address = factoryAddress
                    name = factoryName ?: getString(R.string.unknown_factory)
                    loadKnownAddressLogo(factoryLogoUri, factoryAddress)
                    visible(true)
                }
                noFactoryItem.visible(false)
            } else {
                factoryItem.visible(false)
                noFactoryItem.text = getString(R.string.tx_details_creation_no_factory_used)
                noFactoryItem.visible(true)
            }

            createdItem.label = getString(R.string.tx_details_created)
            createdItem.value = dateTimeText

            etherscanItem.setOnClickListener {
                requireContext().openUrl(
                    getString(
                        R.string.etherscan_transaction_url,
                        transActionHash
                    )
                )
            }

        }
    }
}


