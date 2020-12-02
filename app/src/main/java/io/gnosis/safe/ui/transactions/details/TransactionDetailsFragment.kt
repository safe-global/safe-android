package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewbinding.ViewBinding
import io.gnosis.data.models.transaction.*
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionDetailsBinding
import io.gnosis.safe.databinding.TxDetailsCustomBinding
import io.gnosis.safe.databinding.TxDetailsSettingsChangeBinding
import io.gnosis.safe.databinding.TxDetailsTransferBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.toError
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.Loading
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.ShowError
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.transactions.details.view.TxType
import io.gnosis.safe.utils.*
import pm.gnosis.svalinn.common.utils.openUrl
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import java.math.BigInteger
import javax.inject.Inject

class TransactionDetailsFragment : BaseViewBindingFragment<FragmentTransactionDetailsBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS_DETAILS

    private val navArgs by navArgs<TransactionDetailsFragmentArgs>()
    private val txId by lazy { navArgs.txId }

    @Inject
    lateinit var viewModel: TransactionDetailsViewModel

    @Inject
    lateinit var balanceFormatter: BalanceFormatter

    @Inject
    lateinit var paramSerializer: ParamSerializer

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTransactionDetailsBinding =
        FragmentTransactionDetailsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            backButton.setOnClickListener {
                Navigation.findNavController(root).navigateUp()
            }

            refresh.setOnRefreshListener {
                viewModel.loadDetails(txId)
            }
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (val viewAction = state.viewAction) {
                is UpdateDetails -> {
                    viewAction.txDetails?.let {
                        updateUi(it)
                    }
                }
                is ConfirmationSubmitted -> {
                    binding.txConfirmButtonContainer.visible(false)
                    viewAction.txDetails?.let(::updateUi)
                    snackbar(requireView(), R.string.confirmation_successfully_submitted)
                }
                is Loading -> {
                    showLoading(viewAction.isLoading)
                }
                is ShowError -> {
                    showLoading(false)
                    binding.txConfirmButton.isEnabled = true
                    // only show empty state if we don't have anything to show
                    if (binding.executed.value.isNullOrBlank() && binding.created.value.isNullOrBlank()) {
                        binding.content.visibility = View.GONE
                        binding.contentNoData.root.visible(true)
                    }

                    viewAction.error.let {
                        if (it is TxConfirmationFailed) {
                            val error = it.cause.toError()
                            snackbar(
                                requireView(),
                                error.message(
                                    requireContext(),
                                    R.string.error_description_tx_confirmation
                                )
                            )
                        } else {
                            val error = it.toError()
                            snackbar(
                                requireView(),
                                error.message(
                                    requireContext(),
                                    R.string.error_description_tx_details
                                )
                            )
                        }
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDetails(txId)
    }

    private lateinit var contentBinding: ViewBinding

    private fun showLoading(loading: Boolean) {
        binding.refresh.isRefreshing = loading
    }

    private fun updateUi(txDetails: TransactionDetails) {

        var awaitingYourConfirmation = false

        var nonce: BigInteger? = null
        when (val executionInfo = txDetails.detailedExecutionInfo) {
            is DetailedExecutionInfo.MultisigExecutionDetails -> {

                awaitingYourConfirmation = viewModel.isAwaitingOwnerConfirmation(executionInfo, txDetails.txStatus)

                binding.txConfirmations.visible(true)

                if (awaitingYourConfirmation) {
                    binding.txConfirmButtonContainer.visible(true)
                    binding.txConfirmButton.setOnClickListener {
                        showConfirmDialog(
                            requireContext(),
                            message = R.string.confirm_transaction_dialog_message,
                            confirm = R.string.confirm,
                            confirmColor = R.color.safe_green
                        ) {
                            binding.txConfirmButton.isEnabled = false
                            viewModel.submitConfirmation(txDetails, executionInfo)
                        }
                    }
                } else {
                    binding.txConfirmButton.visible(false)
                }
                binding.txConfirmationsDivider.visible(true)
                binding.txConfirmations.setExecutionData(
                    status = txDetails.txStatus,
                    confirmations = executionInfo.confirmations.sortedBy { it.submittedAt }.map { it.signer },
                    threshold = executionInfo.confirmationsRequired,
                    executor = executionInfo.executor
                )
                nonce = executionInfo.nonce

                binding.created.visible(true)
                binding.createdDivider.visible(true)
                binding.created.value = executionInfo.submittedAt.formatBackendDate()
            }
            is DetailedExecutionInfo.ModuleExecutionDetails -> {
                hideCreatedAndConfirmations()
            }
            else -> {
                hideCreatedAndConfirmations()
            }
        }

        if (txDetails.detailedExecutionInfo != null) {
            binding.advanced.visible(true)
            binding.advancedDivider.visible(true)
            binding.advanced.setOnClickListener {
                val operation = txDetails.txData?.operation?.displayName() ?: ""
                findNavController().navigate(
                    TransactionDetailsFragmentDirections.actionTransactionDetailsFragmentToAdvancedTransactionDetailsFragment(
                        nonce = nonce?.toString() ?: "",
                        operation = operation,
                        hash = txDetails.txHash,
                        safeTxHash = (txDetails.detailedExecutionInfo as? DetailedExecutionInfo.MultisigExecutionDetails)?.safeTxHash
                    )
                )
            }
        } else {
            binding.advanced.visible(false)
            binding.advancedDivider.visible(false)
        }
        if (txDetails.executedAt != null) {
            binding.executed.visible(true)
            binding.executedDivider.visible(true)
            binding.executed.value = txDetails.executedAt!!.formatBackendDate()
        } else {
            binding.executed.visible(false)
            binding.executedDivider.visible(false)
        }

        if (txDetails.txHash != null) {
            binding.etherscan.visible(true)
            binding.etherscan.setOnClickListener {
                requireContext().openUrl(
                    getString(
                        R.string.etherscan_transaction_url,
                        txDetails.txHash
                    )
                )
            }
        } else {
            binding.etherscan.visible(false)
            binding.advancedDivider.visible(false)
        }

        when (val txInfo = txDetails.txInfo) {
            is TransactionInfo.Transfer -> {
                val viewStub = binding.stubTransfer
                if (viewStub.parent != null) {
                    val inflate = viewStub.inflate()
                    contentBinding = TxDetailsTransferBinding.bind(inflate)
                }
                val txDetailsTransferBinding = contentBinding as TxDetailsTransferBinding

                val outgoing = txInfo.direction == TransactionDirection.OUTGOING
                val address = if (outgoing) txInfo.recipient else txInfo.sender

                val txType = if (txInfo.direction == TransactionDirection.INCOMING) {
                    TxType.TRANSFER_INCOMING
                } else {
                    TxType.TRANSFER_OUTGOING
                }
                when (val transferInfo = txInfo.transferInfo) {
                    is TransferInfo.Erc721Transfer -> {
                        txDetailsTransferBinding.txAction.setActionInfo(
                            outgoing = outgoing,
                            amount = txInfo.formattedAmount(balanceFormatter),
                            logoUri = txInfo.logoUri() ?: "",
                            address = address,
                            tokenId = transferInfo.tokenId
                        )

                        txDetailsTransferBinding.contractAddress.address = transferInfo.tokenAddress
                        txDetailsTransferBinding.contractAddress.name = R.string.tx_details_asset_contract
                    }
                    else -> {
                        txDetailsTransferBinding.txAction.setActionInfo(
                            outgoing = outgoing,
                            amount = txInfo.formattedAmount(balanceFormatter),
                            logoUri = txInfo.logoUri() ?: "",
                            address = address
                        )
                        txDetailsTransferBinding.contractAddress.visible(false)
                        txDetailsTransferBinding.contractSeparator.visible(false)
                    }
                }
                txDetailsTransferBinding.txStatus.setStatus(
                    txType.titleRes,
                    txType.iconRes,
                    getStringResForStatus(txDetails.txStatus, awaitingYourConfirmation),
                    getColorForStatus(txDetails.txStatus)
                )
            }
            is TransactionInfo.SettingsChange -> {
                val viewStub = binding.stubSettingsChange
                if (viewStub.parent != null) {
                    contentBinding = TxDetailsSettingsChangeBinding.bind(viewStub.inflate())
                }
                val txDetailsSettingsChangeBinding = contentBinding as TxDetailsSettingsChangeBinding

                txDetailsSettingsChangeBinding.txAction.setActionInfoItems(txInfo.txActionInfoItems())
                txDetailsSettingsChangeBinding.txStatus.setStatus(
                    TxType.MODIFY_SETTINGS.titleRes,
                    TxType.MODIFY_SETTINGS.iconRes,
                    getStringResForStatus(txDetails.txStatus, awaitingYourConfirmation),
                    getColorForStatus(txDetails.txStatus)
                )
            }
            is TransactionInfo.Custom -> {
                val viewStub = binding.stubCustom
                if (viewStub.parent != null) {
                    contentBinding = TxDetailsCustomBinding.bind(viewStub.inflate())
                }
                val txDetailsCustomBinding = contentBinding as TxDetailsCustomBinding

                txDetailsCustomBinding.txAction.setActionInfo(true, txInfo.formattedAmount(balanceFormatter), txInfo.logoUri()!!, txInfo.to)

                val decodedData = txDetails.txData?.dataDecoded
                if (decodedData == null) {
                    txDetailsCustomBinding.txDataDecoded.visible(false)
                    txDetailsCustomBinding.txDataDecodedSeparator.visible(false)
                } else {
                    if (decodedData.method.toLowerCase() == "multisend") {

                        val valueDecoded = (decodedData.parameters?.get(0) as Param.Bytes).valueDecoded

                        txDetailsCustomBinding.txDataDecoded.name = getString(R.string.tx_details_action_multisend, valueDecoded?.size ?: 0)

                        txDetailsCustomBinding.txDataDecoded.setOnClickListener {
                            txDetails.txData?.dataDecoded?.parameters?.getOrNull(0)?.let {
                                if (it is Param.Bytes && it.valueDecoded != null) {
                                    findNavController().navigate(
                                        TransactionDetailsFragmentDirections.actionTransactionDetailsFragmentToTransactionDetailsActionMultisendFragment(
                                            paramSerializer.serializeDecodedValues(it.valueDecoded!!)
                                        )
                                    )
                                }
                            }
                        }
                    } else {

                        txDetailsCustomBinding.txDataDecoded.name = getString(R.string.tx_details_action, txDetails.txData?.dataDecoded?.method)

                        txDetailsCustomBinding.txDataDecoded.setOnClickListener {
                            txDetails.txData?.let {
                                findNavController().navigate(
                                    TransactionDetailsFragmentDirections.actionTransactionDetailsFragmentToTransactionDetailsActionFragment(
                                        it.dataDecoded?.method ?: "",
                                        it.hexData ?: "",
                                        it.dataDecoded?.let { paramSerializer.serializeDecodedData(it) }
                                    )
                                )
                            }
                        }

                    }
                }

                txDetailsCustomBinding.txStatus.setStatus(
                    TxType.CUSTOM.titleRes,
                    TxType.CUSTOM.iconRes,
                    getStringResForStatus(txDetails.txStatus, awaitingYourConfirmation),
                    getColorForStatus(txDetails.txStatus)
                )
                txDetailsCustomBinding.txData.setData(txDetails.txData?.hexData, txInfo.dataSize, getString(R.string.tx_details_data))
            }
        }

        binding.content.visible(true)
        binding.contentNoData.root.visible(false)
        showLoading(false)
    }

    @ColorRes
    private fun getColorForStatus(txStatus: TransactionStatus): Int =
        when (txStatus) {
            TransactionStatus.AWAITING_CONFIRMATIONS -> R.color.safe_pending_orange
            TransactionStatus.AWAITING_EXECUTION -> R.color.safe_pending_orange
            TransactionStatus.SUCCESS -> R.color.safe_green
            TransactionStatus.CANCELLED -> R.color.dark_grey
            TransactionStatus.FAILED -> R.color.tomato
            TransactionStatus.PENDING -> R.color.dark_grey
        }

    @StringRes
    private fun getStringResForStatus(txStatus: TransactionStatus, awaitingYourConfirmation: Boolean): Int =
        when (txStatus) {
            TransactionStatus.AWAITING_CONFIRMATIONS -> if (awaitingYourConfirmation) R.string.tx_status_awaiting_your_confirmation else R.string.tx_status_awaiting_confirmations
            TransactionStatus.AWAITING_EXECUTION -> R.string.tx_status_awaiting_execution
            TransactionStatus.SUCCESS -> R.string.tx_status_success
            TransactionStatus.CANCELLED -> R.string.tx_status_cancelled
            TransactionStatus.FAILED -> R.string.tx_status_failed
            TransactionStatus.PENDING -> R.string.tx_status_pending
        }


    private fun hideCreatedAndConfirmations() {
        binding.txConfirmations.visible(false)
        binding.txConfirmationsDivider.visible(false)
        binding.txConfirmButtonContainer.visible(false)
        binding.created.visible(false)
        binding.createdDivider.visible(false)
    }
}

private fun Operation.displayName(): String =
    when (this) {
        Operation.CALL -> "call"
        Operation.DELEGATE -> "delegateCall"
    }
