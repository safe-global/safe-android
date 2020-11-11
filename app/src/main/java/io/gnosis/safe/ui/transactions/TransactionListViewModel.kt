package io.gnosis.safe.ui.transactions

import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import io.gnosis.data.models.*
import io.gnosis.data.models.transaction.*
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.SafeRepository.Companion.DEFAULT_FALLBACK_HANDLER
import io.gnosis.data.repositories.SafeRepository.Companion.DEFAULT_FALLBACK_HANDLER_DISPLAY_STRING
import io.gnosis.data.repositories.SafeRepository.Companion.DEFAULT_FALLBACK_HANDLER_UNKNOWN_DISPLAY_STRING
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_CHANGE_MASTER_COPY
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_DISABLE_MODULE
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_ENABLE_MODULE
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_SET_FALLBACK_HANDLER
import io.gnosis.data.repositories.SafeRepository.Companion.SAFE_MASTER_COPY_UNKNOWN_DISPLAY_STRING
import io.gnosis.data.repositories.SafeRepository.Companion.masterCopyVersion
import io.gnosis.data.repositories.TokenRepository.Companion.ETH_TOKEN_INFO
import io.gnosis.safe.R
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.transactions.paging.TransactionPagingProvider
import io.gnosis.safe.utils.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger
import javax.inject.Inject

class TransactionListViewModel
@Inject constructor(
    private val transactionsPager: TransactionPagingProvider,
    private val safeRepository: SafeRepository,
    private val ownerCredentialsRepository: OwnerCredentialsRepository,
    private val balanceFormatter: BalanceFormatter,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TransactionsViewState>(appDispatchers) {

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { load(true) }
        }
    }

    override fun initialState(): TransactionsViewState = TransactionsViewState(null, true)

    fun load(safeChange: Boolean = false) {
        safeLaunch {
            val safe = safeRepository.getActiveSafe()
            updateState { TransactionsViewState(isLoading = true, viewAction = if (safeChange) ActiveSafeChanged(safe) else ViewAction.None) }
            if (safe != null) {
                val owner = ownerCredentialsRepository.retrieveCredentials()?.address
                getTransactions(safe.address, owner).collectLatest {
                    updateState {
                        TransactionsViewState(
                            isLoading = false,
                            viewAction = LoadTransactions(it)
                        )
                    }
                }
            } else {
                updateState(forceViewAction = true) { TransactionsViewState(isLoading = false, viewAction = NoSafeSelected) }
            }
        }
    }

    private fun getTransactions(safe: Solidity.Address, owner: Solidity.Address?): Flow<PagingData<TransactionView>> {

        val safeTxItems: Flow<PagingData<TransactionView>> = transactionsPager.getTransactionsStream(safe)
            .map { pagingData ->
                pagingData
                    .map { transaction ->
                        transaction.getTransactionView(safe, transaction.canBeSignedByOwner(owner))
                    }
                    .filter { it !is TransactionView.Unknown }
            }
            .map {
                // insert headers
                it.insertSeparators { before, after ->

                    if (after == null) {
                        // we're at the end of the list
                        return@insertSeparators null  // no separator
                    }

                    if (before == null) {
                        // we're at the beginning of the list

                        return@insertSeparators if (after.isQueued()) {
                            TransactionView.SectionHeader(title = R.string.tx_list_queue)
                        } else if (after.isHistory()) {
                            TransactionView.SectionHeader(title = R.string.tx_list_history)
                        } else {
                            null // no separator
                        }
                    }

                    // we're in the middle of the list
                    if (before.isQueued() && after.isHistory()) {
                        // insert history separator after queued transaction
                        TransactionView.SectionHeader(title = R.string.tx_list_history)
                    } else {
                        null // no separator
                    }
                }
            }
            .cachedIn(viewModelScope)

        return safeTxItems
    }

    private fun Transaction.getTransactionView(
        activeSafe: Solidity.Address,
        awaitingYourConfirmation: Boolean = false
    ): TransactionView {
        return when (val txInfo = txInfo) {
            is TransactionInfo.Transfer -> toTransferView(txInfo, awaitingYourConfirmation)
            is TransactionInfo.SettingsChange -> toSettingsChangeView(txInfo, awaitingYourConfirmation)
            is TransactionInfo.Custom -> toCustomTransactionView(txInfo, activeSafe, awaitingYourConfirmation)
            is TransactionInfo.Creation -> toHistoryCreation(txInfo)
            TransactionInfo.Unknown -> TransactionView.Unknown
        }
    }

    private fun Transaction.toTransferView(txInfo: TransactionInfo.Transfer, awaitingYourConfirmation: Boolean): TransactionView =
        if (isCompleted(txStatus)) historicTransfer(txInfo)
        else queuedTransfer(txInfo, awaitingYourConfirmation)

    private fun Transaction.historicTransfer(txInfo: TransactionInfo.Transfer): TransactionView.Transfer =
        TransactionView.Transfer(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus),
            statusColorRes = statusTextColor(txStatus),
            amountText = formatTransferAmount(txInfo.transferInfo, txInfo.incoming()),
            dateTimeText = timestamp.formatBackendDate(),
            txTypeIcon = if (txInfo.incoming()) R.drawable.ic_arrow_green_10dp else R.drawable.ic_arrow_red_10dp,
            address = if (txInfo.incoming()) txInfo.sender else txInfo.recipient,
            amountColor = if (txInfo.transferInfo.value() > BigInteger.ZERO && txInfo.incoming()) R.color.safe_green else R.color.gnosis_dark_blue,
            alpha = alpha(txStatus),
            nonce = executionInfo?.nonce?.toString() ?: ""
        )

    private fun Transaction.queuedTransfer(txInfo: TransactionInfo.Transfer, awaitingYourConfirmation: Boolean): TransactionView.TransferQueued {
        //TODO work around this bang operator
        val threshold = executionInfo!!.confirmationsRequired
        val thresholdMet = checkThreshold(threshold, executionInfo?.confirmationsSubmitted)
        val incoming = txInfo.incoming()

        return TransactionView.TransferQueued(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus, awaitingYourConfirmation),
            statusColorRes = statusTextColor(txStatus),
            amountText = formatTransferAmount(txInfo.transferInfo, incoming),
            dateTimeText = timestamp.formatBackendDate(),
            txTypeIcon = if (incoming) R.drawable.ic_arrow_green_10dp else R.drawable.ic_arrow_red_10dp,
            address = if (incoming) txInfo.sender else txInfo.recipient,
            amountColor = if (txInfo.transferInfo.value() > BigInteger.ZERO && incoming) R.color.safe_green else R.color.gnosis_dark_blue,
            confirmations = executionInfo?.confirmationsSubmitted ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = executionInfo?.nonce?.toString().orEmpty()
        )
    }

    private fun Transaction.toSettingsChangeView(txInfo: TransactionInfo.SettingsChange, awaitingYourConfirmation: Boolean): TransactionView =
        when {
            txInfo.isQueuedImplementationChange(txStatus) -> queuedImplementationChange(txInfo, awaitingYourConfirmation)
            txInfo.isHistoricImplementationChange(txStatus) -> historicImplementationChange(txInfo)
            txInfo.isQueuedSetFallbackHandler(txStatus) -> queuedSetFallbackHandler(txInfo, awaitingYourConfirmation)
            txInfo.isHistoricSetFallbackHandler(txStatus) -> historicSetFallbackHandler(txInfo)
            txInfo.isQueuedModuleChange(txStatus) -> queuedModuleChange(txInfo, awaitingYourConfirmation)
            txInfo.isHistoricModuleChange(txStatus) -> historicModuleChange(txInfo)
            txInfo.isQueuedSettingsChange(txStatus) -> queuedSettingsChange(txInfo, awaitingYourConfirmation)
            txInfo.isHistoricSettingsChange(txStatus) -> historicSettingsChange(txInfo)
            else -> TransactionView.Unknown
        }

    private fun TransactionInfo.SettingsChange.isHistoricSetFallbackHandler(txStatus: TransactionStatus): Boolean =
        (settingsInfo is SettingsInfo.SetFallbackHandler || METHOD_SET_FALLBACK_HANDLER == dataDecoded.method) && isCompleted(txStatus)

    private fun TransactionInfo.SettingsChange.isQueuedSetFallbackHandler(txStatus: TransactionStatus): Boolean =
        (settingsInfo is SettingsInfo.SetFallbackHandler || METHOD_SET_FALLBACK_HANDLER == dataDecoded.method) && !isCompleted(txStatus)

    private fun TransactionInfo.SettingsChange.isHistoricModuleChange(txStatus: TransactionStatus): Boolean =
        (settingsInfo is SettingsInfo.EnableModule || settingsInfo is SettingsInfo.DisableModule)
                || (METHOD_ENABLE_MODULE == dataDecoded.method || METHOD_DISABLE_MODULE == dataDecoded.method)
                && isCompleted(txStatus)

    private fun TransactionInfo.SettingsChange.isQueuedModuleChange(txStatus: TransactionStatus): Boolean =
        (settingsInfo is SettingsInfo.EnableModule || settingsInfo is SettingsInfo.DisableModule)
                || (METHOD_ENABLE_MODULE == dataDecoded.method || METHOD_DISABLE_MODULE == dataDecoded.method)
                && !isCompleted(txStatus)

    private fun TransactionInfo.SettingsChange.isHistoricImplementationChange(txStatus: TransactionStatus): Boolean =
        (settingsInfo is SettingsInfo.ChangeImplementation || METHOD_CHANGE_MASTER_COPY == dataDecoded.method) && isCompleted(txStatus)

    private fun TransactionInfo.SettingsChange.isQueuedImplementationChange(txStatus: TransactionStatus): Boolean =
        (settingsInfo is SettingsInfo.ChangeImplementation || METHOD_CHANGE_MASTER_COPY == dataDecoded.method) && !isCompleted(txStatus)

    private fun TransactionInfo.SettingsChange.isHistoricSettingsChange(txStatus: TransactionStatus): Boolean =
        settingsInfo !is SettingsInfo.ChangeImplementation || METHOD_CHANGE_MASTER_COPY != dataDecoded.method && isCompleted(txStatus)

    private fun TransactionInfo.SettingsChange.isQueuedSettingsChange(txStatus: TransactionStatus): Boolean =
        settingsInfo !is SettingsInfo.ChangeImplementation || METHOD_CHANGE_MASTER_COPY != dataDecoded.method && !isCompleted(txStatus)

    private fun Transaction.historicSettingsChange(txInfo: TransactionInfo.SettingsChange): TransactionView.SettingsChange =
        TransactionView.SettingsChange(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            method = txInfo.dataDecoded.method,
            alpha = alpha(txStatus),
            nonce = executionInfo?.nonce?.toString().orEmpty()
        )

    private fun Transaction.queuedSettingsChange(
        txInfo: TransactionInfo.SettingsChange,
        awaitingYourConfirmation: Boolean
    ): TransactionView.SettingsChangeQueued {
        //TODO work around the bang operator
        val threshold = executionInfo!!.confirmationsRequired
        val thresholdMet = checkThreshold(threshold, executionInfo?.confirmationsRequired)

        return TransactionView.SettingsChangeQueued(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus, awaitingYourConfirmation),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            settingNameText = txInfo.dataDecoded.method,
            confirmations = executionInfo?.confirmationsSubmitted ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = executionInfo?.nonce?.toString().orEmpty()
        )
    }

    private fun Transaction.historicImplementationChange(txInfo: TransactionInfo.SettingsChange): TransactionView.SettingsChangeVariant {
        val address = (txInfo.settingsInfo as SettingsInfo.ChangeImplementation).implementation
        val version = masterCopyVersion(address) ?: SAFE_MASTER_COPY_UNKNOWN_DISPLAY_STRING

        return TransactionView.SettingsChangeVariant(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            alpha = alpha(txStatus),
            version = version,
            address = address,
            label = R.string.tx_list_change_mastercopy,
            nonce = executionInfo?.nonce.toString()
        )
    }

    private fun Transaction.queuedSetFallbackHandler(
        txInfo: TransactionInfo.SettingsChange,
        awaitingYourConfirmation: Boolean
    ): TransactionView.SettingsChangeVariantQueued {
        val threshold = executionInfo!!.confirmationsRequired
        val thresholdMet = checkThreshold(threshold, executionInfo?.confirmationsSubmitted)
        val address = (txInfo.settingsInfo as SettingsInfo.SetFallbackHandler).handler
        val version =
            if (address == DEFAULT_FALLBACK_HANDLER) DEFAULT_FALLBACK_HANDLER_DISPLAY_STRING
            else DEFAULT_FALLBACK_HANDLER_UNKNOWN_DISPLAY_STRING

        return TransactionView.SettingsChangeVariantQueued(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus, awaitingYourConfirmation),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            version = version,
            address = address,
            label = R.string.tx_list_set_fallback_handler,
            confirmations = executionInfo?.confirmationsSubmitted ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = executionInfo?.nonce?.toString().orEmpty()
        )
    }

    private fun Transaction.historicSetFallbackHandler(txInfo: TransactionInfo.SettingsChange): TransactionView.SettingsChangeVariant {
        val address = (txInfo.settingsInfo as SettingsInfo.SetFallbackHandler).handler
        val version =
            if (address == DEFAULT_FALLBACK_HANDLER) DEFAULT_FALLBACK_HANDLER_DISPLAY_STRING
            else DEFAULT_FALLBACK_HANDLER_UNKNOWN_DISPLAY_STRING

        return TransactionView.SettingsChangeVariant(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            alpha = alpha(txStatus),
            version = version,
            address = address,
            label = R.string.tx_list_set_fallback_handler,
            nonce = executionInfo?.nonce.toString()
        )
    }

    private fun Transaction.queuedModuleChange(
        txInfo: TransactionInfo.SettingsChange,
        awaitingYourConfirmation: Boolean
    ): TransactionView.SettingsChangeVariantQueued {
        val threshold = executionInfo!!.confirmationsRequired
        val thresholdMet = checkThreshold(threshold, executionInfo?.confirmationsSubmitted)
        val settingsInfo = txInfo.settingsInfo
        val (address, label) =
            if (settingsInfo is SettingsInfo.EnableModule) settingsInfo.module to R.string.tx_list_enable_module
            else (settingsInfo as SettingsInfo.DisableModule).module to R.string.tx_list_disable_module

        return TransactionView.SettingsChangeVariantQueued(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus, awaitingYourConfirmation),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            version = "",
            address = address,
            label = label,
            confirmations = executionInfo?.confirmationsSubmitted ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = executionInfo?.nonce?.toString().orEmpty(),
            visibilityVersion = View.INVISIBLE,
            visibilityEllipsizedAddress = View.INVISIBLE,
            visibilityModuleAddress = View.VISIBLE
        )
    }

    private fun Transaction.historicModuleChange(txInfo: TransactionInfo.SettingsChange): TransactionView.SettingsChangeVariant {
        val settingsInfo = txInfo.settingsInfo
        val (address, label) =
            if (settingsInfo is SettingsInfo.EnableModule) settingsInfo.module to R.string.tx_list_enable_module
            else (settingsInfo as SettingsInfo.DisableModule).module to R.string.tx_list_disable_module

        return TransactionView.SettingsChangeVariant(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            alpha = alpha(txStatus),
            address = address,
            label = label,
            version = "",
            visibilityVersion = View.INVISIBLE,
            visibilityEllipsizedAddress = View.INVISIBLE,
            visibilityModuleAddress = View.VISIBLE,
            nonce = executionInfo?.nonce?.toString().orEmpty()
        )
    }

    private fun Transaction.queuedImplementationChange(
        txInfo: TransactionInfo.SettingsChange,
        awaitingYourConfirmation: Boolean
    ): TransactionView.SettingsChangeVariantQueued {
        val threshold = executionInfo!!.confirmationsRequired
        val thresholdMet = checkThreshold(threshold, executionInfo?.confirmationsSubmitted)
        val address = (txInfo.settingsInfo as SettingsInfo.ChangeImplementation).implementation
        val version = masterCopyVersion(address) ?: SAFE_MASTER_COPY_UNKNOWN_DISPLAY_STRING

        return TransactionView.SettingsChangeVariantQueued(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus, awaitingYourConfirmation),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            confirmations = executionInfo?.confirmationsSubmitted ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = executionInfo?.nonce?.toString().orEmpty(),
            version = version,
            address = address,
            label = R.string.tx_list_change_mastercopy
        )
    }

    private fun Transaction.toCustomTransactionView(
        txInfo: TransactionInfo.Custom,
        safeAddress: Solidity.Address,
        awaitingYourConfirmation: Boolean
    ): TransactionView =
        if (!isCompleted(txStatus)) queuedCustomTransaction(txInfo, safeAddress, awaitingYourConfirmation)
        else historicCustomTransaction(txInfo, safeAddress)

    private fun Transaction.historicCustomTransaction(
        txInfo: TransactionInfo.Custom,
        safeAddress: Solidity.Address
    ): TransactionView.CustomTransaction {
        val isIncoming: Boolean = txInfo.to == safeAddress
        return TransactionView.CustomTransaction(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            address = txInfo.to,
            dataSizeText = if (txInfo.dataSize >= 0) "${txInfo.dataSize} bytes" else "",
            amountText = balanceFormatter.formatAmount(txInfo.value, isIncoming, ETH_TOKEN_INFO.decimals, ETH_TOKEN_INFO.symbol),
            amountColor = if (txInfo.value > BigInteger.ZERO && isIncoming) R.color.safe_green else R.color.gnosis_dark_blue,
            alpha = alpha(txStatus),
            nonce = executionInfo?.nonce?.toString() ?: ""
        )
    }

    private fun Transaction.queuedCustomTransaction(
        txInfo: TransactionInfo.Custom,
        safeAddress: Solidity.Address,
        awaitingYourConfirmation: Boolean
    ): TransactionView.CustomTransactionQueued {
        val isIncoming: Boolean = txInfo.to == safeAddress
        //TODO work around this bang operator
        val threshold = executionInfo!!.confirmationsRequired
        val thresholdMet = checkThreshold(threshold, executionInfo?.confirmationsSubmitted)

        return TransactionView.CustomTransactionQueued(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus, awaitingYourConfirmation),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            address = txInfo.to,
            confirmations = executionInfo?.confirmationsSubmitted ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = executionInfo?.nonce?.toString() ?: "",
            dataSizeText = if (txInfo.dataSize >= 0) "${txInfo.dataSize} bytes" else "",
            amountText = balanceFormatter.formatAmount(txInfo.value, isIncoming, ETH_TOKEN_INFO.decimals, ETH_TOKEN_INFO.symbol),
            amountColor = if (txInfo.value > BigInteger.ZERO && isIncoming) R.color.safe_green else R.color.gnosis_dark_blue
        )
    }

    private fun Transaction.toHistoryCreation(txInfo: TransactionInfo.Creation): TransactionView.Creation =
        TransactionView.Creation(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            label = R.string.tx_list_creation,
            creationDetails = TransactionView.CreationDetails(
                statusText = displayString(txStatus),
                statusColorRes = statusTextColor(txStatus),
                dateTimeText = timestamp.formatBackendDate(),
                creator = txInfo.creator.asEthereumAddressString(),
                factory = txInfo.factory?.asEthereumAddressString(),
                implementation = txInfo.implementation?.asEthereumAddressString(),
                transactionHash = txInfo.transactionHash
            )
        )

    private fun isCompleted(status: TransactionStatus): Boolean =
        when (status) {
            TransactionStatus.AWAITING_CONFIRMATIONS,
            TransactionStatus.AWAITING_EXECUTION,
            TransactionStatus.PENDING -> false
            TransactionStatus.SUCCESS,
            TransactionStatus.FAILED,
            TransactionStatus.CANCELLED -> true
        }

    @StringRes
    private fun displayString(status: TransactionStatus, awaitingYourConfirmation: Boolean = false): Int =
        when (status) {
            TransactionStatus.AWAITING_CONFIRMATIONS -> if (awaitingYourConfirmation) R.string.tx_status_awaiting_your_confirmation else R.string.tx_status_awaiting_confirmations
            TransactionStatus.AWAITING_EXECUTION -> R.string.tx_status_awaiting_execution
            TransactionStatus.CANCELLED -> R.string.tx_status_cancelled
            TransactionStatus.FAILED -> R.string.tx_status_failed
            TransactionStatus.SUCCESS -> R.string.tx_status_success
            TransactionStatus.PENDING -> R.string.tx_status_pending
        }

    private fun formatTransferAmount(transferInfo: TransferInfo, incoming: Boolean): String {
        val symbol = transferInfo.symbol().let { symbol ->
            if (symbol.isNullOrEmpty()) {
                getDefaultSymbol(transferInfo.type)
            } else {
                symbol
            }
        }
        return balanceFormatter.formatAmount(transferInfo.value(), incoming, transferInfo.decimals() ?: 0, symbol)
    }

    private fun getDefaultSymbol(type: TransferType?): String = when (type) {
        TransferType.ERC721 -> DEFAULT_ERC721_SYMBOL
        TransferType.ERC20 -> DEFAULT_ERC20_SYMBOL
        else -> ""
    }

    private fun statusTextColor(status: TransactionStatus): Int {
        return when (status) {
            TransactionStatus.SUCCESS -> R.color.safe_green
            TransactionStatus.CANCELLED -> R.color.dark_grey
            TransactionStatus.AWAITING_EXECUTION,
            TransactionStatus.AWAITING_CONFIRMATIONS,
            TransactionStatus.PENDING -> R.color.safe_pending_orange
            else -> R.color.safe_failed_red
        }
    }

    private fun alpha(txStatus: TransactionStatus): Float =
        when (txStatus) {
            TransactionStatus.FAILED, TransactionStatus.CANCELLED -> OPACITY_HALF
            else -> OPACITY_FULL
        }

    private fun checkThreshold(safeThreshold: Int, txThreshold: Int?): Boolean {
        return txThreshold?.let {
            it >= safeThreshold
        } ?: false
    }

    companion object {
        const val OPACITY_FULL = 1.0F
        const val OPACITY_HALF = 0.5F
    }
}

data class TransactionsViewState(
    override var viewAction: BaseStateViewModel.ViewAction?,
    val isLoading: Boolean
) : BaseStateViewModel.State

data class LoadTransactions(
    val newTransactions: PagingData<TransactionView>
) : BaseStateViewModel.ViewAction

data class ActiveSafeChanged(
    val activeSafe: Safe?
) : BaseStateViewModel.ViewAction

object NoSafeSelected : BaseStateViewModel.ViewAction

fun TransactionView.isQueued() = when (status) {
    TransactionStatus.PENDING,
    TransactionStatus.AWAITING_CONFIRMATIONS,
    TransactionStatus.AWAITING_EXECUTION -> true
    else -> false
}

fun TransactionView.isHistory() = when (status) {
    TransactionStatus.CANCELLED,
    TransactionStatus.FAILED,
    TransactionStatus.SUCCESS -> true
    else -> false
}

fun Solidity.Address.getVersionForAddress(): String = masterCopyVersion(this) ?: SAFE_MASTER_COPY_UNKNOWN_DISPLAY_STRING

fun Transaction.canBeSignedByOwner(ownerAddress: Solidity.Address?): Boolean {
    return executionInfo?.missingSigners?.contains(ownerAddress) == true
}

//TODO add unit tests
fun TransactionInfo.Transfer.incoming(): Boolean = direction != TransactionDirection.OUTGOING
