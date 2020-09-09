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
import io.gnosis.safe.ui.transactions.details.view.TxStatusView
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
                titleRes = TxStatusView.TxType.CREATION.titleRes,
                iconRes = TxStatusView.TxType.CREATION.iconRes,
                statusTextRes = statusTextRes,
                statusColorRes = statusColorRes
            )
            txHashItem.name = getString(R.string.tx_details_advanced_hash)
            txHashItem.value = transActionHash
            txHashItem.setOnClickListener {
                context?.copyToClipboard(context?.getString(R.string.hash_copied)!!, txHashItem.value.toString()) {
                    snackbar(view = root, textId = R.string.copied_success)
                }
            }

            creatorItemTitle.text = getString(R.string.tx_details_creation_creator_address)
            creatorItem.address = creator!!.asEthereumAddress()


            if (implementation != null) {
                implementationSeparator.visible(true)
                implementationTitle.visible(true)
                implementationItem.visible(true)

                implementationTitle.text = getString(R.string.tx_details_creation_implementation_used)
                implementationItem.setAddress(implementation!!.asEthereumAddress(), false)
            } else {
                implementationSeparator.visibility = View.GONE
                implementationTitle.visibility = View.GONE
                implementationItem.visibility = View.GONE
            }

            if (factory != null) {
                factoryTitle.visible(true)
                factoryItem.visible(true)
                factorySeparator.visible(true)

                factoryTitle.text = getString(R.string.tx_details_creation_factory_used)
                factoryItem.address = factory!!.asEthereumAddress()
            } else {
                factoryTitle.visibility = View.GONE
                factoryItem.visibility = View.GONE
                factorySeparator.visibility = View.GONE
            }

            createdItem.name = getString(R.string.tx_details_created)
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


