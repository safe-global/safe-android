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
    private val implementation by lazy { navArgs.implementation }
    private val factory by lazy { navArgs.factory }
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
            creatorItem.address = creator!!.asEthereumAddress()

            implementationTitle.text = getString(R.string.tx_details_creation_implementation_used)
            if (implementation != null) {
                implementationItem.setAddress(implementation!!.asEthereumAddress(), false)
                noImplementationItem.visible(false)
            } else {
                noImplementationItem.text = getString(R.string.tx_details_creation_no_implementation_available)
                noImplementationItem.visible(true)
            }

            factoryTitle.text = getString(R.string.tx_details_creation_factory_used)
            if (factory != null) {
                factoryItem.address = factory!!.asEthereumAddress()
                factoryItem.visible(true)

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


