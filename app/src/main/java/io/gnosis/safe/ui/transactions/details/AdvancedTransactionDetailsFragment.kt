package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import io.gnosis.data.models.transaction.Operation
import io.gnosis.data.models.transaction.ParamType
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionDetailsAdvancedBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.transactions.details.view.getArrayItem
import io.gnosis.safe.ui.transactions.details.view.getDataItem
import io.gnosis.safe.ui.transactions.details.view.getLabeledAddressItem
import io.gnosis.safe.ui.transactions.details.view.getLabeledValueItem
import io.gnosis.safe.utils.ParamSerializer
import io.gnosis.safe.utils.dpToPx
import io.gnosis.safe.utils.toColor
import pm.gnosis.svalinn.common.utils.copyToClipboard
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class AdvancedTransactionDetailsFragment : BaseViewBindingFragment<FragmentTransactionDetailsAdvancedBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS_DETAILS_ADVANCED

    private val navArgs by navArgs<AdvancedTransactionDetailsFragmentArgs>()
    private val chain by lazy { navArgs.chain }
    private val hash by lazy { navArgs.hash }
    private val data by lazy { navArgs.data?.let { paramSerializer.deserializeData(it) } }
    private val executionInfo by lazy { navArgs.executionInfo?.let { paramSerializer.deserializeExecutionInfo(it) } }

    @Inject
    lateinit var paramSerializer: ParamSerializer

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

            if (hash.isNullOrBlank()) {
                hashItem.visible(false)
                chainDataHeader.visible(false)
            } else {
                hashItem.value = hash
                hashItem.setOnClickListener {
                    context?.copyToClipboard(context?.getString(R.string.hash_copied)!!, hashItem.value.toString()) {
                        snackbar(view = root, textId = R.string.copied_success)
                    }
                }
            }

            if (data == null && executionInfo == null) {
                safeDataHeader.visible(false)
            } else {

                data?.let {
                    content.addView(
                            requireContext().getLabeledAddressItem(chain, getString(R.string.tx_details_advanced_to), it.to.value, if (it.to.name != null) it.to else null)
                    )
                    content.addView(
                            requireContext().getLabeledValueItem(getString(R.string.tx_details_advanced_value), it.value.toString())
                    )
                    content.addView(
                            requireContext().getDataItem(getString(R.string.tx_details_advanced_data), it.hexData ?: "")
                    )
                    content.addView(
                            requireContext().getLabeledValueItem(
                                    getString(R.string.tx_details_advanced_operation),
                                    getString(R.string.tx_details_advanced_operation_value, it.operation.id, it.operation.displayName())
                            )
                    )
                }

                executionInfo?.let {

                    content.addView(
                            getDivider()
                    )

                    val safeTxHashItem = requireContext().getLabeledValueItem(getString(R.string.tx_details_advanced_safe_tx_hash), it.safeTxHash)
                    safeTxHashItem.setOnClickListener {
                        context?.copyToClipboard(context?.getString(R.string.hash_copied)!!, safeTxHashItem.value.toString()) {
                            snackbar(view = root, textId = R.string.copied_success)
                        }
                    }
                    content.addView(safeTxHashItem)

                    content.addView(
                            requireContext().getLabeledValueItem(getString(R.string.tx_details_advanced_nonce), it.nonce.toString())
                    )

                    content.addView(
                            getDivider()
                    )

                    content.addView(
                            requireContext().getLabeledValueItem(getString(R.string.tx_details_advanced_safe_tx_gas), it.safeTxGas.toString())
                    )
                    content.addView(
                            requireContext().getLabeledValueItem(getString(R.string.tx_details_advanced_base_gas), it.baseGas.toString())
                    )
                    content.addView(
                            requireContext().getLabeledValueItem(getString(R.string.tx_details_advanced_gas_price), it.gasPrice.toString())
                    )
                    content.addView(
                            requireContext().getLabeledAddressItem(chain, getString(R.string.tx_details_advanced_gas_token), it.gasToken, null)
                    )
                    //TODO: parse refundReceiver
//                content.addView(
//                        getLabeledAddressItem(getString(R.string.tx_details_advanced_operation_refundReceiver), it.refundReceiver, null)
//                )

                    if (it.confirmations.isNotEmpty()) {
                        content.addView(
                                getDivider()
                        )
                        content.addView(
                                requireContext().getArrayItem(chain, getString(R.string.tx_details_advanced_signatures), it.confirmations.map { it.signature }, ParamType.BYTES, "bytes", null)
                        )
                    }
                }
            }

            content.addView(
                    getBottomMargin()
            )
        }
    }

    private fun getDivider(): View {
        val item = View(requireContext())
        val height = resources.getDimension(R.dimen.default_large_margin).toInt()
        val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        layoutParams.setMargins(0, dpToPx(16), 0, 0)
        item.layoutParams = layoutParams
        item.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.separator))
        return item
    }

    private fun getBottomMargin(): View {
        val item = View(requireContext())
        val height = resources.getDimension(R.dimen.default_large_margin).toInt()
        val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        item.layoutParams = layoutParams
        return item
    }
}

private fun Operation.displayName(): String =
        when (this) {
            Operation.CALL -> "call"
            Operation.DELEGATE -> "delegateCall"
        }

