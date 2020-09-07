package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionDetailsCreationBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.transactions.details.view.TxStatusView

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
                titleRes =  TxStatusView.TxType.CREATION.titleRes,
                iconRes = TxStatusView.TxType.CREATION.iconRes,
                statusTextRes = statusTextRes,
                statusColorRes = statusColorRes
            )

//            nonceItem.value = nonce
//            operationItem.value = operation
//            if (hash.isNullOrBlank()) {
//                hashItem.visible(false)
//                hashSeparator.visible(false)
//            } else {
//                hashItem.value = hash
//                hashItem.setOnClickListener {
//                    context?.copyToClipboard(context?.getString(R.string.address_copied)!!, hashItem.value.toString()) {
//                        snackbar(view = root, textId = R.string.copied_success)
//                    }
//                }
//            }
        }
    }
}


