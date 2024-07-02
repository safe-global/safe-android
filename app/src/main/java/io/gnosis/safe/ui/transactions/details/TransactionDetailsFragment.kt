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
import io.gnosis.safe.databinding.*
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.errorSnackbar
import io.gnosis.safe.toError
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.*
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.transactions.details.view.ActionInfoItem
import io.gnosis.safe.ui.transactions.details.view.TxType
import io.gnosis.safe.ui.transactions.details.viewdata.TransactionDetailsViewData
import io.gnosis.safe.ui.transactions.details.viewdata.TransactionInfoViewData
import io.gnosis.safe.utils.*
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.openUrl
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger
import javax.inject.Inject

class TransactionDetailsFragment : BaseViewBindingFragment<FragmentTransactionDetailsBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS_DETAILS

    private val navArgs by navArgs<TransactionDetailsFragmentArgs>()
    private val chain by lazy { navArgs.chain }
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

            chainRibbon.text = chain.name
            chainRibbon.setTextColor(chain.textColor.toColor(requireContext(), R.color.white))
            chainRibbon.setBackgroundColor(chain.backgroundColor.toColor(requireContext(), R.color.primary))

            backButton.setOnClickListener {
                Navigation.findNavController(root).navigateUp()
            }
            refresh.setOnRefreshListener {
                // Use txDetails.txId here to generate a non cached url
                viewModel.loadDetails(viewModel.txDetails?.txId ?: txId)
            }
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (val viewAction = state.viewAction) {
                is UpdateDetails -> {
                    viewAction.txDetails?.let { transactionDetailsViewData ->
                        updateUi(transactionDetailsViewData)
                    }
                }
                is ConfirmConfirmation -> {
                    binding.txConfirmButton.isEnabled = false
                    viewModel.submitConfirmation(viewModel.txDetails!!, viewAction.owner)
                }
                is ConfirmationSubmitted -> {
                    viewAction.txDetails?.let { transactionDetailsViewData ->
                        updateUi(transactionDetailsViewData)
                    }
                    snackbar(requireView(), R.string.confirmation_successfully_submitted)
                }
                is Loading -> {
                    showLoading(viewAction.isLoading)
                }
                is NavigateTo -> {
                    findNavController().navigate(viewAction.navDirections)
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
                        val error = it.toError()
                        if (error.trackingRequired) {
                            tracker.logException(it)
                        }
                        when (it) {
                            is TxConfirmationFailed -> errorSnackbar(
                                    requireView(),
                                    error.message(requireContext(), R.string.error_description_tx_confirmation)
                            )
                            else -> errorSnackbar(requireView(), error.message(requireContext(), R.string.error_description_tx_details))
                        }
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (ownerSelected() != null) {
            viewModel.resumeFlow(ownerSelected()!!, ownerSigned())
            resetOwnerData()
        } else {
            // Use txDetails.txId here to generate a non cached url
            viewModel.loadDetails(viewModel.txDetails?.txId ?: txId)
        }
    }

    private lateinit var contentBinding: ViewBinding

    private fun showLoading(loading: Boolean) {
        binding.refresh.isRefreshing = loading
    }

    private fun updateUi(txDetails: TransactionDetailsViewData) {
        var nonce: BigInteger? = null
        val awaitingConfirmations = txDetails.txStatus == TransactionStatus.AWAITING_CONFIRMATIONS
        when (val executionInfo = txDetails.detailedExecutionInfo) {
            is DetailedExecutionInfo.MultisigExecutionDetails -> {
                binding.txConfirmations.visible(true)

                val hasBeenRejected = executionInfo.rejectors?.isNotEmpty() ?: false
                val isRejection = txDetails.txInfo is TransactionInfoViewData.Rejection
                val awaitingExecution = txDetails.txStatus == TransactionStatus.AWAITING_EXECUTION
                val buttonState = ButtonStateHelper(
                        hasBeenRejected = hasBeenRejected,
                        awaitingConfirmations = awaitingConfirmations,
                        isRejection = isRejection,
                        awaitingExecution = awaitingExecution,
                        canSign = txDetails.canSign,
                        canExecute = txDetails.canExecute,
                        hasOwnerKey = txDetails.hasOwnerKey,
                        nextInLine = txDetails.nextInLine
                )

                if (buttonState.buttonContainerIsVisible()) {
                    binding.scrollView.setPadding(0, 0, 0, resources.getDimension(R.dimen.item_tx_m_height).toInt())
                }
                binding.txButtonContainer.visible(buttonState.buttonContainerIsVisible())

                binding.txConfirmButton.visible(buttonState.confirmationButtonIsVisible())
                binding.txConfirmButton.text = getString(if (buttonState.canExecute) R.string.execute else R.string.confirm)
                binding.txConfirmButton.isEnabled = buttonState.confirmationButtonIsEnabled()
                binding.txConfirmButton.setOnClickListener {
                    if (buttonState.canExecute) {
                        // start execution flow
                        findNavController().navigate(
                            TransactionDetailsFragmentDirections.actionTransactionDetailsFragmentToTxReviewFragment(
                                chain = chain,
                                txDetails = txDetails
                            )
                        )
                    } else {
                        viewModel.startConfirmationFlow()
                    }
                    binding.txConfirmButton.isEnabled = false
                }
                binding.txRejectButton.visible(buttonState.rejectionButtonIsVisible())
                binding.txRejectButton.isEnabled = buttonState.rejectionButtonIsEnabled()
                binding.txRejectButton.setOnClickListener {
                    binding.txRejectButton.isEnabled = false
                    findNavController().navigate(
                            TransactionDetailsFragmentDirections.actionTransactionDetailsFragmentToConfirmRejectionFragment(txId)
                    )
                }
                binding.spaceBetweenButtons.visible(buttonState.spacerIsVisible())

                binding.txConfirmationsDivider.visible(true)
                binding.txConfirmations.setExecutionData(
                        chain = chain,
                        showChainPrefix = viewModel.isChainPrefixPrependEnabled(),
                        copyChainPrefix = viewModel.isChainPrefixCopyEnabled(),
                        rejection = isRejection,
                        status = txDetails.txStatus,
                        confirmations = executionInfo.confirmations.sortedBy { it.submittedAt }.map { it.signer.value },
                        threshold = executionInfo.confirmationsRequired,
                        executor = executionInfo.executor?.value,
                        localOwners = txDetails.owners
                )
                nonce = executionInfo.nonce

                binding.created.visible(true)
                binding.createdDivider.visible(true)
                binding.created.value = executionInfo.submittedAt.formatBackendDateTime()
            }
            is DetailedExecutionInfo.ModuleExecutionDetails -> {
                hideCreatedAndConfirmations()
            } else -> {
                hideCreatedAndConfirmations()
            }
        }

        if (txDetails.detailedExecutionInfo != null || txDetails.txHash != null) {
            binding.advanced.visible(true)
            binding.advancedDivider.visible(true)
            binding.advanced.setOnClickListener {
                findNavController().navigate(
                        TransactionDetailsFragmentDirections.actionTransactionDetailsFragmentToAdvancedTransactionDetailsFragment(
                                chain = chain,
                                hash = txDetails.txHash,
                                data = paramSerializer.serializeData(txDetails.txData),
                                executionInfo = paramSerializer.serializeExecutionInfo(txDetails.detailedExecutionInfo)
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
            binding.executed.value = txDetails.executedAt.formatBackendDateTime()
        } else {
            binding.executed.visible(false)
            binding.executedDivider.visible(false)
        }

        if (txDetails.txHash != null) {
            binding.etherscan.visible(true)
            binding.etherscan.setOnClickListener {
                BlockExplorer.forChain(chain)?.showTransaction(requireContext(), txDetails.txHash)
            }
        } else {
            binding.etherscan.visible(false)
            binding.advancedDivider.visible(false)
        }

        if (txDetails.txInfo !is TransactionInfoViewData.SwapOrder && txDetails.txInfo !is TransactionInfoViewData.SwapTransfer) {
            binding.etherscanDivider.visible(false)
            binding.orderLink.visible(false)
        }

        when (val txInfo = txDetails.txInfo) {
            is TransactionInfoViewData.Transfer -> {
                val viewStub = binding.stubTransfer
                if (viewStub.parent != null) {
                    val inflate = viewStub.inflate()
                    contentBinding = TxDetailsTransferBinding.bind(inflate)
                }
                val txDetailsTransferBinding = contentBinding as TxDetailsTransferBinding
                with(txDetailsTransferBinding) {

                    val outgoing = txInfo.direction == TransactionDirection.OUTGOING
                    val address = txInfo.address

                    val txType = if (!outgoing) {
                        TxType.TRANSFER_INCOMING
                    } else {
                        TxType.TRANSFER_OUTGOING
                    }
                    when (val transferInfo = txInfo.transferInfo) {
                        is TransferInfo.Erc721Transfer -> {
                            txAction.setActionInfo(
                                    chain = chain,
                                    outgoing = outgoing,
                                    amount = txInfo.formattedAmount(chain, balanceFormatter),
                                    logoUri = txInfo.logoUri(chain) ?: "",
                                    address = address,
                                    showChainPrefix = viewModel.isChainPrefixPrependEnabled(),
                                    copyChainPrefix = viewModel.isChainPrefixCopyEnabled(),
                                    tokenId = transferInfo.tokenId,
                                    addressName = txInfo.addressName,
                                    addressUri = txInfo.addressUri
                            )
                            contractAddress.setAddress(
                                chain = chain,
                                value = transferInfo.tokenAddress,
                                showChainPrefix = viewModel.isChainPrefixPrependEnabled(),
                                copyChainPrefix = viewModel.isChainPrefixCopyEnabled()
                            )
                            contractAddress.name = getString(R.string.tx_details_asset_contract)
                        }
                        else -> {
                            txAction.setActionInfo(
                                    chain = chain,
                                    outgoing = outgoing,
                                    amount = txInfo.formattedAmount(chain, balanceFormatter),
                                    logoUri = txInfo.logoUri(chain) ?: "",
                                    address = address,
                                    showChainPrefix = viewModel.isChainPrefixPrependEnabled(),
                                    copyChainPrefix = viewModel.isChainPrefixCopyEnabled(),
                                    addressName = txInfo.addressName,
                                    addressUri = txInfo.addressUri
                            )
                            contractAddress.visible(false)
                            contractSeparator.visible(false)
                        }
                    }
                    txStatus.setStatus(
                            txType.titleRes,
                            txType.iconRes,
                            getStringResForStatus(txDetails.txStatus, txDetails.canSign && awaitingConfirmations),
                            getColorForStatus(txDetails.txStatus)
                    )
                }
            }
            is TransactionInfoViewData.SettingsChange -> {
                val viewStub = binding.stubSettingsChange
                if (viewStub.parent != null) {
                    contentBinding = TxDetailsSettingsChangeBinding.bind(viewStub.inflate())
                }
                val txDetailsSettingsChangeBinding = contentBinding as TxDetailsSettingsChangeBinding
                with(txDetailsSettingsChangeBinding) {
                    txAction.setActionInfoItems(
                        chain = chain,
                        showChainPrefix = viewModel.isChainPrefixPrependEnabled(),
                        copyChainPrefix = viewModel.isChainPrefixCopyEnabled(),
                        actionInfoItems = txInfo.txActionInfoItems(requireContext().resources)
                    )
                    txStatus.setStatus(
                            TxType.MODIFY_SETTINGS.titleRes,
                            TxType.MODIFY_SETTINGS.iconRes,
                            getStringResForStatus(txDetails.txStatus, txDetails.canSign && awaitingConfirmations),
                            getColorForStatus(txDetails.txStatus)
                    )
                }
            }
            is TransactionInfoViewData.Custom -> {
                val viewStub = binding.stubCustom
                if (viewStub.parent != null) {
                    contentBinding = TxDetailsCustomBinding.bind(viewStub.inflate())
                }
                val txDetailsCustomBinding = contentBinding as TxDetailsCustomBinding
                with(txDetailsCustomBinding) {
                    txAction.setActionInfo(
                            chain = chain,
                            outgoing = true,
                            amount = txInfo.formattedAmount(chain, balanceFormatter),
                            logoUri = txInfo.logoUri(chain) ?: "",
                            address = txInfo.to,
                            showChainPrefix = viewModel.isChainPrefixPrependEnabled(),
                            copyChainPrefix = viewModel.isChainPrefixCopyEnabled(),
                            addressUri = txInfo.actionInfoAddressUri,
                            addressName = txInfo.actionInfoAddressName
                    )
                    val decodedData = txDetails.txData?.dataDecoded
                    if (decodedData == null) {
                        txDataDecoded.visible(false)
                        txDataDecodedSeparator.visible(false)
                    } else {

                        if (decodedData.method.lowercase() == "multisend") {

                            val valueDecoded = (decodedData.parameters?.get(0) as Param.Bytes).valueDecoded

                            txDataDecoded.name = getString(R.string.tx_details_action_multisend, valueDecoded?.size ?: 0)
                            txDataDecoded.setOnClickListener {
                                txDetails.txData.dataDecoded?.parameters?.getOrNull(0)?.let { param ->
                                    if (param is Param.Bytes && param.valueDecoded != null) {
                                        findNavController().navigate(
                                                TransactionDetailsFragmentDirections.actionTransactionDetailsFragmentToTransactionDetailsActionMultisendFragment(
                                                        chain,
                                                        paramSerializer.serializeDecodedValues(param.valueDecoded!!),
                                                        paramSerializer.serializeAddressInfoIndex(txDetails.txData.addressInfoIndex)
                                                )
                                        )
                                    }
                                }
                            }
                        } else {

                            txDataDecoded.name = getString(R.string.tx_details_action, txDetails.txData.dataDecoded?.method)
                            txDataDecoded.setOnClickListener {
                                txDetails.txData.let {
                                    findNavController().navigate(
                                            TransactionDetailsFragmentDirections.actionTransactionDetailsFragmentToTransactionDetailsActionFragment(
                                                    chain = chain,
                                                    action = it.dataDecoded?.method ?: "",
                                                    data = it.hexData ?: "",
                                                    decodedData = it.dataDecoded?.let { paramSerializer.serializeDecodedData(it) },
                                                    addressInfoIndex = paramSerializer.serializeAddressInfoIndex(it.addressInfoIndex)
                                            )
                                    )
                                }
                            }

                        }
                    }

                    txStatus.setStatus(
                            title = txInfo.statusTitle ?: resources.getString(TxType.CUSTOM.titleRes),
                            iconUrl = txInfo.statusIconUri,
                            defaultIconRes = TxType.CUSTOM.iconRes,
                            statusTextRes = getStringResForStatus(txDetails.txStatus, txDetails.canSign && awaitingConfirmations),
                            statusColorRes = getColorForStatus(txDetails.txStatus),
                            safeApp = txInfo.safeApp
                    )
                    txData.setData(txDetails.txData?.hexData, txInfo.dataSize, getString(R.string.tx_details_data))
                }
            }

            is TransactionInfoViewData.Rejection -> {
                val viewStub = binding.stubRejection
                if (viewStub.parent != null) {
                    contentBinding = TxDetailsRejectionBinding.bind(viewStub.inflate())
                }
                val txDetailsRejectionBinding = contentBinding as TxDetailsRejectionBinding
                with(txDetailsRejectionBinding) {
                    if (txDetails.txStatus.isCompleted()) {
                        txRejectionInfo.text = getString(R.string.tx_details_rejection_info_historic, nonce)
                        txPaymentReasonLink.visible(false)
                    } else {
                        txRejectionInfo.text = getString(R.string.tx_details_rejection_info_queued, nonce)
                        txPaymentReasonLink.visible(true)
                        txPaymentReasonLink.setLink(
                                url = getString(R.string.tx_details_rejection_payment_reason_link),
                                urlText = getString(R.string.tx_details_rejection_payment_reason),
                                linkIcon = R.drawable.ic_external_link_green_16dp,
                                underline = true
                        )
                    }
                    txStatus.setStatus(
                            TxType.REJECTION.titleRes,
                            TxType.REJECTION.iconRes,
                            getStringResForStatus(txDetails.txStatus, txDetails.canSign && awaitingConfirmations),
                            getColorForStatus(txDetails.txStatus)
                    )
                }
            }
            is TransactionInfoViewData.SwapOrder -> {
                val viewStub = binding.stubSettingsChange
                if (viewStub.parent != null) {
                    contentBinding = TxDetailsSettingsChangeBinding.bind(viewStub.inflate())
                }
                val txDetailsSettingsChangeBinding = contentBinding as TxDetailsSettingsChangeBinding
                with(txDetailsSettingsChangeBinding) {
                    txAction.visible(true)
                    txAction.setActionInfoItems(
                        chain = chain,
                        showChainPrefix = viewModel.isChainPrefixPrependEnabled(),
                        copyChainPrefix = viewModel.isChainPrefixCopyEnabled(),
                        actionInfoItems = listOf<ActionInfoItem>(ActionInfoItem.Value(
                            itemLabel = R.string.tx_status_type_custom ,
                            value = txInfo.displayDescription))
                        )

                    txStatus.setStatus(
                        title = txInfo.displayDescription,
                        defaultIconRes = TxType.SWAP_ORDER.iconRes,
                        statusTextRes = getStringResForStatus(txDetails.txStatus, txDetails.canSign && awaitingConfirmations),
                        statusColorRes = getColorForStatus(txDetails.txStatus)
                    )
                    binding.etherscanDivider.visible(true)
                    binding.orderLink.visible(true)
                    binding.orderLink.setOnClickListener {
                        requireContext().openUrl(txInfo.explorerUrl)
                    }
                }
            }
            is TransactionInfoViewData.SwapTransfer -> {
                val viewStub = binding.stubSettingsChange
                if (viewStub.parent != null) {
                    contentBinding = TxDetailsSettingsChangeBinding.bind(viewStub.inflate())
                }
                val txDetailsSettingsChangeBinding = contentBinding as TxDetailsSettingsChangeBinding
                with(txDetailsSettingsChangeBinding) {
                    txAction.visible(true)
                    txAction.setActionInfoItems(
                        chain = chain,
                        showChainPrefix = viewModel.isChainPrefixPrependEnabled(),
                        copyChainPrefix = viewModel.isChainPrefixCopyEnabled(),
                        actionInfoItems = listOf<ActionInfoItem>(ActionInfoItem.Value(
                            itemLabel = R.string.tx_status_type_custom ,
                            value = txInfo.displayDescription))
                    )

                    txStatus.setStatus(
                        title = txInfo.displayDescription,
                        defaultIconRes = TxType.SWAP_TRANSFER.iconRes,
                        statusTextRes = getStringResForStatus(txDetails.txStatus, txDetails.canSign && awaitingConfirmations),
                        statusColorRes = getColorForStatus(txDetails.txStatus)
                    )
                    binding.etherscanDivider.visible(true)
                    binding.orderLink.visible(true)
                    binding.orderLink.setOnClickListener {
                        requireContext().openUrl(txInfo.explorerUrl)
                    }
                }
            }
            is TransactionInfoViewData.TwapOrder -> {
                val viewStub = binding.stubSettingsChange
                if (viewStub.parent != null) {
                    contentBinding = TxDetailsSettingsChangeBinding.bind(viewStub.inflate())
                }
                val txDetailsSettingsChangeBinding = contentBinding as TxDetailsSettingsChangeBinding
                with(txDetailsSettingsChangeBinding) {
                    txAction.visible(true)
                    txAction.setActionInfoItems(
                        chain = chain,
                        showChainPrefix = viewModel.isChainPrefixPrependEnabled(),
                        copyChainPrefix = viewModel.isChainPrefixCopyEnabled(),
                        actionInfoItems = listOf<ActionInfoItem>(ActionInfoItem.Value(
                            itemLabel = R.string.tx_status_type_custom ,
                            value = txInfo.displayDescription))
                    )

                    txStatus.setStatus(
                        title = txInfo.displayDescription,
                        defaultIconRes = TxType.TWAP_ORDER.iconRes,
                        statusTextRes = getStringResForStatus(txDetails.txStatus, txDetails.canSign && awaitingConfirmations),
                        statusColorRes = getColorForStatus(txDetails.txStatus)
                    )
                }
            }
        }

        binding.content.visible(true)
        binding.contentNoData.root.visible(false)
        showLoading(false)
    }

    @ColorRes
    private fun getColorForStatus(txStatus: TransactionStatus): Int =
            when (txStatus) {
                TransactionStatus.AWAITING_CONFIRMATIONS -> R.color.warning
                TransactionStatus.AWAITING_EXECUTION -> R.color.warning
                TransactionStatus.SUCCESS -> R.color.success
                TransactionStatus.CANCELLED -> R.color.label_secondary
                TransactionStatus.FAILED -> R.color.error
                TransactionStatus.PENDING -> R.color.warning
            }

    @StringRes
    private fun getStringResForStatus(txStatus: TransactionStatus, needsYourConfirmation: Boolean): Int =
            when (txStatus) {
                TransactionStatus.AWAITING_CONFIRMATIONS -> if (needsYourConfirmation) R.string.tx_status_needs_your_confirmation else R.string.tx_status_needs_confirmations
                TransactionStatus.AWAITING_EXECUTION -> R.string.tx_status_needs_execution
                TransactionStatus.SUCCESS -> R.string.tx_status_success
                TransactionStatus.CANCELLED -> R.string.tx_status_cancelled
                TransactionStatus.FAILED -> R.string.tx_status_failed
                TransactionStatus.PENDING -> R.string.tx_status_pending
            }


    private fun hideCreatedAndConfirmations() {
        binding.txConfirmations.visible(false)
        binding.txConfirmationsDivider.visible(false)
        binding.txButtonContainer.visible(false)
        binding.created.visible(false)
        binding.createdDivider.visible(false)
    }

    private fun ownerSelected(): Solidity.Address? {
        return findNavController().currentBackStackEntry?.savedStateHandle?.get<String>(SafeOverviewBaseFragment.OWNER_SELECTED_RESULT)
                ?.asEthereumAddress()
    }

    private fun ownerSigned(): String? {
        return findNavController().currentBackStackEntry?.savedStateHandle?.get<String>(SafeOverviewBaseFragment.OWNER_SIGNED_RESULT)
    }

    private fun resetOwnerData() {
        findNavController().currentBackStackEntry?.savedStateHandle?.set(SafeOverviewBaseFragment.OWNER_SELECTED_RESULT, null)
        findNavController().currentBackStackEntry?.savedStateHandle?.set(SafeOverviewBaseFragment.OWNER_SIGNED_RESULT, null)
    }
}
