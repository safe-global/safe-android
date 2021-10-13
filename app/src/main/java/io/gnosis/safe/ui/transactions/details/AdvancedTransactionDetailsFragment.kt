package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionDetailsAdvancedBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.toColor
import pm.gnosis.svalinn.common.utils.copyToClipboard
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible

class AdvancedTransactionDetailsFragment : BaseViewBindingFragment<FragmentTransactionDetailsAdvancedBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS_DETAILS_ADVANCED

    private val navArgs by navArgs<AdvancedTransactionDetailsFragmentArgs>()
    private val chain by lazy { navArgs.chain }
    private val nonce by lazy { navArgs.nonce }
    private val operation by lazy { navArgs.operation }
    private val hash by lazy { navArgs.hash }
    private val safeTxHash by lazy { navArgs.safeTxHash }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTransactionDetailsAdvancedBinding =
            FragmentTransactionDetailsAdvancedBinding.inflate(inflater, container, false)

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {

            chainRibbon.text = chain.name
            chainRibbon.setTextColor(chain.textColor.toColor(requireContext(), R.color.white))
            chainRibbon.setBackgroundColor(chain.backgroundColor.toColor(requireContext(), R.color.primary))

            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
            }
            if (nonce.isBlank()) {
                nonceItem.visible(false)
                nonceSeparator.visible(false)
            } else {
                nonceItem.value = nonce
            }
            if (operation.isBlank()) {
                operationItem.visible(false)
                operationSeparator.visible(false)
            } else {
                operationItem.value = operation
            }
            if (hash.isNullOrBlank()) {
                hashItem.visible(false)
                hashSeparator.visible(false)
            } else {
                hashItem.value = hash
                hashItem.setOnClickListener {
                    context?.copyToClipboard(context?.getString(R.string.hash_copied)!!, hashItem.value.toString()) {
                        snackbar(view = root, textId = R.string.copied_success)
                    }
                }
            }
            if (safeTxHash.isNullOrBlank()) {
                safeTxHashItem.visible(false)
                safeTxHashSeparator.visible(false)
            } else {
                safeTxHashItem.value = safeTxHash
                safeTxHashItem.setOnClickListener {
                    context?.copyToClipboard(context?.getString(R.string.hash_copied)!!, safeTxHashItem.value.toString()) {
                        snackbar(view = root, textId = R.string.copied_success)
                    }
                }
            }
        }
    }
}


