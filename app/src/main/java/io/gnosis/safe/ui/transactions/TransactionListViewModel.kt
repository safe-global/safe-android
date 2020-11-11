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
                val safeInfo = safeRepository.getSafeInfo(safe.address)
                val owner = ownerCredentialsRepository.retrieveCredentials()?.address
                getTransactions(safe.address, safeInfo, owner).collectLatest {
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

    private fun getTransactions(safe: Solidity.Address, safeInfo: SafeInfo, owner: Solidity.Address?): Flow<PagingData<TransactionView>> {

        val safeTxItems: Flow<PagingData<TransactionView>> = transactionsPager.getTransactionsStream(safe)
            .map { pagingData ->
                pagingData
                    .map { transaction ->
                        getTransactionView(transaction, safeInfo, transaction.canBeSignedByOwner(owner))
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

    fun getTransactionView(transaction: Transaction, awaitingYourConfirmation: Boolean = false): TransactionView {
        return when (val txInfo = transaction.txInfo) {
            is TransactionInfo.Transfer -> transaction.toTransferView(txInfo, awaitingYourConfirmation)
            is TransactionInfo.Custom -> TODO()
            is TransactionInfo.SettingsChange -> TODO()
            is TransactionInfo.Creation -> transaction.toHistoryCreation(txInfo)
            TransactionInfo.Unknown -> TransactionView.Unknown
        }
    }

    fun getTransactionView(transaction: Transaction, safeInfo: SafeInfo, awaitingYourConfirmation: Boolean = false): TransactionView {
        return when {
            transaction.txInfo is TransactionInfo.SettingsChange && isQueuedMastercopyChange(transaction) ->
                queuedMastercopyChange(transaction, safeInfo.threshold, awaitingYourConfirmation)
            transaction.txInfo is TransactionInfo.SettingsChange && isHistoricMastercopyChange(transaction) ->
                historicMastercopyChange(transaction)
            transaction.txInfo is TransactionInfo.SettingsChange && isQueuedSetFallbackHandler(transaction) ->
                queuedSetFallbackHandler(transaction, safeInfo.threshold, awaitingYourConfirmation)
            transaction.txInfo is TransactionInfo.SettingsChange && isHistoricSetFallbackHandler(transaction) ->
                historicSetFallbackHandler(transaction)
            transaction.txInfo is TransactionInfo.SettingsChange && isQueuedModuleChange(transaction) ->
                queuedModuleChange(transaction, safeInfo.threshold, awaitingYourConfirmation)
            transaction.txInfo is TransactionInfo.SettingsChange && isHistoricModuleChange(transaction) ->
                historicModuleChange(transaction)
            transaction.txInfo is TransactionInfo.SettingsChange && isQueuedSettingsChange(transaction) ->
                queuedSettingsChange(transaction, safeInfo.threshold, awaitingYourConfirmation)
            transaction.txInfo is TransactionInfo.SettingsChange && isHistoricSettingsChange(transaction) ->
                historicSettingsChange(transaction)
            transaction.txInfo is TransactionInfo.Custom && isQueuedCustomTransaction(transaction) ->
                queuedCustomTransaction(transaction, safeInfo, awaitingYourConfirmation)
            transaction.txInfo is TransactionInfo.Custom && isHistoricCustomTransaction(transaction) ->
                historicCustomTransaction(transaction, safeInfo)
            else -> TransactionView.Unknown
        }
    }

    private fun isHistoricSetFallbackHandler(settingsChange: SettingsChange): Boolean {
        return METHOD_SET_FALLBACK_HANDLER == settingsChange.dataDecoded.method && isCompleted(settingsChange.status)
    }

    private fun isQueuedSetFallbackHandler(settingsChange: SettingsChange): Boolean {
        return METHOD_SET_FALLBACK_HANDLER == settingsChange.dataDecoded.method && !isCompleted(settingsChange.status)
    }

    private fun isHistoricModuleChange(settingsChange: SettingsChange): Boolean {
        return (METHOD_ENABLE_MODULE == settingsChange.dataDecoded.method || METHOD_DISABLE_MODULE == settingsChange.dataDecoded.method) && isCompleted(
            settingsChange.status
        )
    }

    private fun isQueuedModuleChange(settingsChange: SettingsChange): Boolean {
        return (METHOD_ENABLE_MODULE == settingsChange.dataDecoded.method || METHOD_DISABLE_MODULE == settingsChange.dataDecoded.method) && !isCompleted(
            settingsChange.status
        )
    }

    private fun isHistoricMastercopyChange(settingsChange: SettingsChange): Boolean {
        return METHOD_CHANGE_MASTER_COPY == settingsChange.dataDecoded.method && isCompleted(settingsChange.status)
    }

    private fun isQueuedMastercopyChange(settingsChange: SettingsChange): Boolean {
        return METHOD_CHANGE_MASTER_COPY == settingsChange.dataDecoded.method && !isCompleted(settingsChange.status)
    }

    private fun isHistoricCustomTransaction(custom: Custom): Boolean {
        return isCompleted(custom.status)
    }

    private fun isQueuedCustomTransaction(custom: Custom): Boolean {
        return !isCompleted(custom.status)
    }

    private fun isHistoricSettingsChange(settingsChange: SettingsChange): Boolean {
        return METHOD_CHANGE_MASTER_COPY != settingsChange.dataDecoded.method && isCompleted(settingsChange.status)
    }

    private fun isQueuedSettingsChange(settingsChange: SettingsChange): Boolean {
        return METHOD_CHANGE_MASTER_COPY != settingsChange.dataDecoded.method && !isCompleted(settingsChange.status)
    }

    private fun isCompleted(status: TransactionStatus): Boolean =
        when (status) {
            TransactionStatus.AWAITING_CONFIRMATIONS,
            TransactionStatus.AWAITING_EXECUTION,
            TransactionStatus.PENDING -> false
            TransactionStatus.SUCCESS,
            TransactionStatus.FAILED,
            TransactionStatus.CANCELLED -> true
        }

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
            alpha = alpha(this),
            nonce = executionInfo?.nonce?.toString() ?: ""
        )

    private fun Transaction.queuedTransfer(txInfo: TransactionInfo.Transfer, awaitingYourConfirmation: Boolean): TransactionView? {
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
            dateTimeText = timestamp.formatBackendDate() ?: "",
            txTypeIcon = if (incoming) R.drawable.ic_arrow_green_10dp else R.drawable.ic_arrow_red_10dp,
            address = if (incoming) txInfo.sender else txInfo.recipient,
            amountColor = if (txInfo.transferInfo.value() > BigInteger.ZERO && incoming) R.color.safe_green else R.color.gnosis_dark_blue,
            confirmations = executionInfo?.confirmationsSubmitted ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = executionInfo?.nonce?.toString() ?: ""
        )
    }

    private fun historicSettingsChange(transaction: SettingsChange): TransactionView.SettingsChange =
        TransactionView.SettingsChange(
            id = transaction.id,
            status = transaction.status,
            statusText = displayString(transaction.status),
            statusColorRes = statusTextColor(transaction.status),
            dateTimeText = transaction.date?.formatBackendDate() ?: "",
            method = transaction.dataDecoded.method,
            alpha = alpha(transaction),
            nonce = transaction.nonce.toString()
        )

    private fun queuedSettingsChange(
        transaction: SettingsChange,
        threshold: Int,
        awaitingYourConfirmation: Boolean
    ): TransactionView.SettingsChangeQueued {
        val thresholdMet = checkThreshold(threshold, transaction.confirmations)

        return TransactionView.SettingsChangeQueued(
            id = transaction.id,
            status = transaction.status,
            statusText = displayString(transaction.status, awaitingYourConfirmation),
            statusColorRes = statusTextColor(transaction.status),
            dateTimeText = transaction.date?.formatBackendDate() ?: "",
            settingNameText = transaction.dataDecoded.method,
            confirmations = transaction.confirmations ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = transaction.nonce.toString()
        )
    }

    private fun historicMastercopyChange(transaction: SettingsChange): TransactionView.SettingsChangeVariant {

        val address = getAddress(transaction, "_masterCopy")
        val version = masterCopyVersion(address) ?: SAFE_MASTER_COPY_UNKNOWN_DISPLAY_STRING

        return TransactionView.SettingsChangeVariant(
            id = transaction.id,
            status = transaction.status,
            statusText = displayString(transaction.status),
            statusColorRes = statusTextColor(transaction.status),
            dateTimeText = transaction.date?.formatBackendDate() ?: "",
            alpha = alpha(transaction),
            version = version,
            address = address,
            label = R.string.tx_list_change_mastercopy,
            nonce = transaction.nonce.toString()
        )
    }

    private fun queuedSetFallbackHandler(
        transaction: SettingsChange,
        threshold: Int,
        awaitingYourConfirmation: Boolean
    ): TransactionView.SettingsChangeVariantQueued {
        val thresholdMet = checkThreshold(threshold, transaction.confirmations)
        val address = getAddress(transaction, "handler")
        val version =
            if (address == DEFAULT_FALLBACK_HANDLER) DEFAULT_FALLBACK_HANDLER_DISPLAY_STRING else DEFAULT_FALLBACK_HANDLER_UNKNOWN_DISPLAY_STRING


        return TransactionView.SettingsChangeVariantQueued(
            id = transaction.id,
            status = transaction.status,
            statusText = displayString(transaction.status, awaitingYourConfirmation),
            statusColorRes = statusTextColor(transaction.status),
            dateTimeText = transaction.date?.formatBackendDate() ?: "",
            version = version,
            address = address,
            label = R.string.tx_list_set_fallback_handler,
            confirmations = transaction.confirmations ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = transaction.nonce.toString()
        )
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

    private fun historicSetFallbackHandler(transaction: SettingsChange): TransactionView.SettingsChangeVariant {
        val address = getAddress(transaction, "handler")
        val version =
            if (address == DEFAULT_FALLBACK_HANDLER) DEFAULT_FALLBACK_HANDLER_DISPLAY_STRING else DEFAULT_FALLBACK_HANDLER_UNKNOWN_DISPLAY_STRING

        return TransactionView.SettingsChangeVariant(
            id = transaction.id,
            status = transaction.status,
            statusText = displayString(transaction.status),
            statusColorRes = statusTextColor(transaction.status),
            dateTimeText = transaction.date?.formatBackendDate() ?: "",
            alpha = alpha(transaction),
            version = version,
            address = address,
            label = R.string.tx_list_set_fallback_handler,
            nonce = transaction.nonce.toString()
        )
    }

    private fun queuedModuleChange(
        transaction: SettingsChange,
        threshold: Int,
        awaitingYourConfirmation: Boolean
    ): TransactionView.SettingsChangeVariantQueued {
        val thresholdMet = checkThreshold(threshold, transaction.confirmations)
        val address = getAddress(transaction, "module")
        val label = if (transaction.dataDecoded.method == "enableModule") R.string.tx_list_enable_module else R.string.tx_list_disable_module

        return TransactionView.SettingsChangeVariantQueued(
            id = transaction.id,
            status = transaction.status,
            statusText = displayString(transaction.status, awaitingYourConfirmation),
            statusColorRes = statusTextColor(transaction.status),
            dateTimeText = transaction.date?.formatBackendDate() ?: "",
            version = "",
            address = address,
            label = label,
            confirmations = transaction.confirmations ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = transaction.nonce.toString(),
            visibilityVersion = View.INVISIBLE,
            visibilityEllipsizedAddress = View.INVISIBLE,
            visibilityModuleAddress = View.VISIBLE
        )
    }

    private fun historicModuleChange(transaction: SettingsChange): TransactionView.SettingsChangeVariant {

        val address = getAddress(transaction, "module")
        val label = if (transaction.dataDecoded.method == "enableModule") R.string.tx_list_enable_module else R.string.tx_list_disable_module

        return TransactionView.SettingsChangeVariant(
            id = transaction.id,
            status = transaction.status,
            statusText = displayString(transaction.status),
            statusColorRes = statusTextColor(transaction.status),
            dateTimeText = transaction.date?.formatBackendDate() ?: "",
            alpha = alpha(transaction),
            address = address,
            label = label,
            version = "",
            visibilityVersion = View.INVISIBLE,
            visibilityEllipsizedAddress = View.INVISIBLE,
            visibilityModuleAddress = View.VISIBLE,
            nonce = transaction.nonce.toString()
        )
    }

    private fun queuedMastercopyChange(
        transaction: SettingsChange,
        threshold: Int,
        awaitingYourConfirmation: Boolean
    ): TransactionView.SettingsChangeVariantQueued {
        val thresholdMet = checkThreshold(threshold, transaction.confirmations)
        val address = getAddress(transaction, "_masterCopy")
        val version = masterCopyVersion(address) ?: SAFE_MASTER_COPY_UNKNOWN_DISPLAY_STRING

        return TransactionView.SettingsChangeVariantQueued(
            id = transaction.id,
            status = transaction.status,
            statusText = displayString(transaction.status, awaitingYourConfirmation),
            statusColorRes = statusTextColor(transaction.status),
            dateTimeText = transaction.date?.formatBackendDate() ?: "",
            confirmations = transaction.confirmations ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = transaction.nonce.toString(),
            version = version,
            address = address,
            label = R.string.tx_list_change_mastercopy
        )
    }

    private fun getAddress(transaction: SettingsChange, key: String): Solidity.Address? =
        transaction.dataDecoded.parameters.getAddressValueByName(key)

    private fun historicCustomTransaction(custom: Custom, safeInfo: SafeInfo): TransactionView.CustomTransaction {
        val isIncoming: Boolean = custom.address == safeInfo.address
        return TransactionView.CustomTransaction(
            id = custom.id,
            status = custom.status,
            statusText = displayString(custom.status),
            statusColorRes = statusTextColor(custom.status),
            dateTimeText = custom.date?.formatBackendDate() ?: "",
            address = custom.address,
            dataSizeText = if (custom.dataSize >= 0) "${custom.dataSize} bytes" else "",
            amountText = balanceFormatter.formatAmount(custom.value, isIncoming, ETH_TOKEN_INFO.decimals, ETH_TOKEN_INFO.symbol),
            amountColor = if (custom.value > BigInteger.ZERO && isIncoming) R.color.safe_green else R.color.gnosis_dark_blue,
            alpha = alpha(custom),
            nonce = custom.nonce?.toString() ?: ""
        )
    }

    private fun queuedCustomTransaction(
        custom: Custom,
        safeInfo: SafeInfo,
        awaitingYourConfirmation: Boolean
    ): TransactionView.CustomTransactionQueued {
        val isIncoming: Boolean = custom.address == safeInfo.address
        val thresholdMet = checkThreshold(safeInfo.threshold, custom.confirmations)
        return TransactionView.CustomTransactionQueued(
            id = custom.id,
            status = custom.status,
            statusText = displayString(custom.status, awaitingYourConfirmation),
            statusColorRes = statusTextColor(custom.status),
            dateTimeText = custom.date?.formatBackendDate() ?: "",
            address = custom.address,
            confirmations = custom.confirmations ?: 0,
            threshold = safeInfo.threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = custom.nonce?.toString() ?: "",
            dataSizeText = if (custom.dataSize >= 0) "${custom.dataSize} bytes" else "",
            amountText = balanceFormatter.formatAmount(custom.value, isIncoming, ETH_TOKEN_INFO.decimals, ETH_TOKEN_INFO.symbol),
            amountColor = if (custom.value > BigInteger.ZERO && isIncoming) R.color.safe_green else R.color.gnosis_dark_blue
        )
    }

    private fun Transaction.toTransferView(txInfo: TransactionInfo.Transfer, awaitingYourConfirmation: Boolean): TransactionView =
//        transaction.txInfo is TransactionInfo.Transfer && isHistoricTransfer(transaction) ->
//        historicTransfer(transaction)
//        transaction.txInfo is TransactionInfo.Transfer && isQueuedTransfer(transaction) ->
//        queuedTransfer(transaction, safeInfo, awaitingYourConfirmation)
        if (isCompleted(txStatus)) historicTransfer(txInfo) else queuedTransfer()


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

    private fun alpha(transaction: Transaction): Float =
        when (transaction.txStatus) {
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
