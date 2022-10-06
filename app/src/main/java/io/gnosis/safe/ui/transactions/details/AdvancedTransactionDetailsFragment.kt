package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.data.models.transaction.DetailedExecutionInfo
import io.gnosis.data.models.transaction.Operation
import io.gnosis.data.models.transaction.Param
import io.gnosis.data.models.transaction.ParamType
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionDetailsAdvancedBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.view.SettingItem
import io.gnosis.safe.ui.transactions.details.view.*
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
                            requireContext().getDivider()
                    )
                    content.addView(
                            requireContext().getDataItem(getString(R.string.tx_details_advanced_data), it.hexData ?: "")
                    )
                    content.addView(
                            requireContext().getDivider()
                    )

                    data!!.dataDecoded?.let { dataDecoded ->
                        if (dataDecoded.method.toLowerCase() == "multisend") {
                            val valueDecoded = (dataDecoded.parameters?.get(0) as Param.Bytes).valueDecoded
                            val decodedDataItemName = getString(R.string.tx_details_action_multisend, valueDecoded?.size ?: 0)
                            val decodedDataItem = getDecodedDataItem(decodedDataItemName)
                            decodedDataItem.setOnClickListener {
                                dataDecoded.parameters?.getOrNull(0)?.let { param ->
                                    if (param is Param.Bytes && param.valueDecoded != null) {
                                        findNavController().navigate(
                                                AdvancedTransactionDetailsFragmentDirections.actionAdvancedTransactionDetailsFragmentToTransactionDetailsActionMultisendFragment(
                                                        chain,
                                                        paramSerializer.serializeDecodedValues(param.valueDecoded!!),
                                                        paramSerializer.serializeAddressInfoIndex(data!!.addressInfoIndex)
                                                )
                                        )
                                    }
                                }
                            }

                            content.addView(decodedDataItem)
                            content.addView(
                                    requireContext().getDivider()
                            )
                        } else {
                            val decodedDataItemName = getString(R.string.tx_details_action, dataDecoded.method)
                            val decodedDataItem = getDecodedDataItem(decodedDataItemName)
                            decodedDataItem.setOnClickListener {
                                findNavController().navigate(
                                        AdvancedTransactionDetailsFragmentDirections.actionAdvancedTransactionDetailsFragmentToTransactionDetailsActionFragment(
                                                chain = chain,
                                                action = dataDecoded.method,
                                                data = data!!.hexData ?: "",
                                                decodedData = paramSerializer.serializeDecodedData(dataDecoded),
                                                addressInfoIndex = paramSerializer.serializeAddressInfoIndex(data!!.addressInfoIndex)
                                        )
                                )
                            }

                            content.addView(decodedDataItem)
                            content.addView(
                                    requireContext().getDivider()
                            )
                        }
                    }

                    content.addView(
                            requireContext().getLabeledValueItem(
                                    getString(R.string.tx_details_advanced_operation),
                                    getString(R.string.tx_details_advanced_operation_value, it.operation.id, it.operation.displayName())
                            )
                    )
                }

                executionInfo?.let {

                    when (executionInfo) {

                        is DetailedExecutionInfo.ModuleExecutionDetails -> {

                            val info = executionInfo as DetailedExecutionInfo.ModuleExecutionDetails

                            content.addView(
                                    getModuleDataHeader()
                            )
                            content.addView(
                                    requireContext().getLabeledAddressItem(chain, getString(R.string.tx_details_advanced_module), info.address.value, if (info.address.name != null) info.address else null)
                            )
                        }

                        is DetailedExecutionInfo.MultisigExecutionDetails -> {

                            val info = executionInfo as DetailedExecutionInfo.MultisigExecutionDetails

                            content.addView(
                                    requireContext().getDividerBig()
                            )

                            val safeTxHashItem = requireContext().getLabeledValueItem(getString(R.string.tx_details_advanced_safe_tx_hash), info.safeTxHash)
                            safeTxHashItem.setOnClickListener {
                                context?.copyToClipboard(context?.getString(R.string.hash_copied)!!, safeTxHashItem.value.toString()) {
                                    snackbar(view = root, textId = R.string.copied_success)
                                }
                            }
                            content.addView(safeTxHashItem)

                            content.addView(
                                    requireContext().getLabeledValueItem(getString(R.string.tx_details_advanced_nonce), info.nonce.toString())
                            )

                            content.addView(
                                    requireContext().getDividerBig()
                            )

                            content.addView(
                                    requireContext().getLabeledValueItem(getString(R.string.tx_details_advanced_safe_tx_gas), info.safeTxGas.toString())
                            )
                            content.addView(
                                    requireContext().getLabeledValueItem(getString(R.string.tx_details_advanced_base_gas), info.baseGas.toString())
                            )
                            content.addView(
                                    requireContext().getLabeledValueItem(getString(R.string.tx_details_advanced_gas_price), info.gasPrice.toString())
                            )
                            content.addView(
                                    requireContext().getLabeledAddressItem(chain, getString(R.string.tx_details_advanced_gas_token), info.gasToken, null)
                            )

                            info.refundReceiver?.let {
                                content.addView(
                                        requireContext().getLabeledAddressItem(chain, getString(R.string.tx_details_advanced_refundReceiver), it.value, null)
                                )
                            }

                            if (info.confirmations.isNotEmpty()) {
                                content.addView(
                                        requireContext().getDividerBig()
                                )
                                content.addView(
                                        requireContext().getArrayItem(chain, getString(R.string.tx_details_advanced_signatures), info.confirmations.map { it.signature }, ParamType.BYTES, "bytes", null)
                                )
                            }
                        }
                    }
                }
            }

            content.addView(
                    getBottomMargin()
            )
        }
    }

    private fun getDecodedDataItem(name: String): View {
        val item = SettingItem(requireContext())
        val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(60))
        layoutParams.setMargins(0, 0, 0, -dpToPx(16))
        item.layoutParams = layoutParams
        item.openable = true
        item.name = name
        return item
    }

    private fun getModuleDataHeader(): View {
        val item = TextView(requireContext(), null, 0, R.style.Header)
        val height = resources.getDimension(R.dimen.header_height).toInt()
        val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        layoutParams.setMargins(0, dpToPx(16), 0, 0)
        item.layoutParams = layoutParams
        item.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_primary))
        item.isAllCaps = true
        item.text = getString(R.string.tx_details_advanced_module_data)
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

