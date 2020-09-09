package io.gnosis.safe.ui.transactions

import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import io.gnosis.data.models.*
import io.gnosis.data.models.Transaction.*
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_CHANGE_MASTER_COPY
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_DISABLE_MODULE
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_ENABLE_MODULE
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_SET_FALLBACK_HANDLER
import io.gnosis.data.repositories.SafeRepository.Companion.SAFE_MASTER_COPY_UNKNOWN_DISPLAY_STRING
import io.gnosis.data.repositories.SafeRepository.Companion.masterCopyVersion
import io.gnosis.data.repositories.TokenRepository.Companion.ETH_SERVICE_TOKEN_INFO
import io.gnosis.data.repositories.getValueByName
import io.gnosis.safe.R
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.transactions.paging.TransactionPagingProvider
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.formatBackendDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger
import javax.inject.Inject

class TransactionListViewModel
@Inject constructor(
    private val transactionsPager: TransactionPagingProvider,
    private val safeRepository: SafeRepository,
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
                getTransactions(safe.address, safeInfo).collectLatest {
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

    private fun getTransactions(safe: Solidity.Address, safeInfo: SafeInfo): Flow<PagingData<TransactionView>> {

        val safeTxItems: Flow<PagingData<TransactionView>> = transactionsPager.getTransactionsStream(safe, safeInfo)
            .map { pagingData ->
                pagingData
                    .map { transaction -> getTransactionView(transaction, safeInfo) }
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

    fun getTransactionView(transaction: Transaction, safeInfo: SafeInfo): TransactionView {
        return when {
            transaction is Transfer && isHistoricTransfer(transaction) -> historicTransfer(transaction)
            transaction is Transfer && isQueuedTransfer(transaction) -> queuedTransfer(transaction, safeInfo)
            transaction is SettingsChange && isQueuedMastercopyChange(transaction) -> queuedMastercopyChange(
                transaction,
                safeInfo.threshold
            )
            transaction is SettingsChange && isHistoricMastercopyChange(transaction) -> historicMastercopyChange(transaction)
            transaction is SettingsChange && isQueuedSetFallbackHandler(transaction) -> queuedSetFallbackHandler(
                transaction,
                safeInfo.threshold
            )
            transaction is SettingsChange && isHistoricSetFallbackHandler(transaction) -> historicSetFallbackHandler(transaction)
            transaction is SettingsChange && isQueuedModuleChange(transaction) -> queuedModuleChange(transaction, safeInfo.threshold)
            transaction is SettingsChange && isHistoricModuleChange(transaction) -> historicModuleChange(transaction)
            transaction is SettingsChange && isQueuedSettingsChange(transaction) -> queuedSettingsChange(transaction, safeInfo.threshold)
            transaction is SettingsChange && isHistoricSettingsChange(transaction) -> historicSettingsChange(transaction)
            transaction is Custom && isQueuedCustomTransaction(transaction) -> queuedCustomTransaction(transaction, safeInfo)
            transaction is Custom && isHistoricCustomTransaction(transaction) -> historicCustomTransaction(transaction, safeInfo)
            transaction is Creation -> historicCreation(transaction)
            else -> TransactionView.Unknown
        } as TransactionView
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

    private fun isHistoricTransfer(transfer: Transfer): Boolean {
        return isCompleted(transfer.status)
    }

    private fun isQueuedTransfer(transfer: Transfer): Boolean {
        return !isCompleted(transfer.status)
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

    private fun historicTransfer(
        transfer: Transfer
    ): TransactionView.Transfer {

        return TransactionView.Transfer(
            id = transfer.id,
            status = transfer.status,
            statusText = displayString(transfer.status),
            statusColorRes = statusTextColor(transfer.status),
            amountText = formatTransferAmount(transfer, transfer.incoming),
            dateTimeText = transfer.date?.formatBackendDate() ?: "",
            txTypeIcon = if (transfer.incoming) R.drawable.ic_arrow_green_10dp else R.drawable.ic_arrow_red_10dp,
            address = if (transfer.incoming) transfer.sender else transfer.recipient,
            amountColor = if (transfer.value > BigInteger.ZERO && transfer.incoming) R.color.safe_green else R.color.gnosis_dark_blue,
            alpha = alpha(transfer),
            nonce = transfer.nonce?.toString() ?: ""
        )
    }

    private fun queuedTransfer(transfer: Transfer, safeInfo: SafeInfo): TransactionView? {
        val thresholdMet = checkThreshold(safeInfo.threshold, transfer.confirmations)

        return TransactionView.TransferQueued(
            id = transfer.id,
            status = transfer.status,
            statusText = displayString(transfer.status),
            statusColorRes = statusTextColor(transfer.status),
            amountText = formatTransferAmount(transfer, transfer.incoming),
            dateTimeText = transfer.date?.formatBackendDate() ?: "",
            txTypeIcon = if (transfer.incoming) R.drawable.ic_arrow_green_10dp else R.drawable.ic_arrow_red_10dp,
            address = if (transfer.incoming) transfer.sender else transfer.recipient,
            amountColor = if (transfer.value > BigInteger.ZERO && transfer.incoming) R.color.safe_green else R.color.gnosis_dark_blue,
            confirmations = transfer.confirmations ?: 0,
            threshold = safeInfo.threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = transfer.nonce?.toString() ?: ""
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

    private fun queuedSettingsChange(transaction: SettingsChange, threshold: Int): TransactionView.SettingsChangeQueued {
        val thresholdMet = checkThreshold(threshold, transaction.confirmations)

        return TransactionView.SettingsChangeQueued(
            id = transaction.id,
            status = transaction.status,
            statusText = displayString(transaction.status),
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

    private fun queuedSetFallbackHandler(transaction: SettingsChange, threshold: Int): TransactionView.SettingsChangeVariantQueued {
        val thresholdMet = checkThreshold(threshold, transaction.confirmations)
        val address = getAddress(transaction, "handler")

        return TransactionView.SettingsChangeVariantQueued(
            id = transaction.id,
            status = transaction.status,
            statusText = displayString(transaction.status),
            statusColorRes = statusTextColor(transaction.status),
            dateTimeText = transaction.date?.formatBackendDate() ?: "",
            version = "DefaultFallbackHandler",
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
    private fun displayString(status: TransactionStatus): Int =
        when (status) {
            TransactionStatus.AWAITING_CONFIRMATIONS -> R.string.tx_list_awaiting_confirmations
            TransactionStatus.AWAITING_EXECUTION -> R.string.tx_list_awaiting_execution
            TransactionStatus.CANCELLED -> R.string.tx_list_cancelled
            TransactionStatus.FAILED -> R.string.tx_list_failed
            TransactionStatus.SUCCESS -> R.string.tx_list_success
            TransactionStatus.PENDING -> R.string.tx_list_pending
        }

    private fun historicSetFallbackHandler(transaction: SettingsChange): TransactionView.SettingsChangeVariant {
        val address = getAddress(transaction, "handler")

        return TransactionView.SettingsChangeVariant(
            id = transaction.id,
            status = transaction.status,
            statusText = displayString(transaction.status),
            statusColorRes = statusTextColor(transaction.status),
            dateTimeText = transaction.date?.formatBackendDate() ?: "",
            alpha = alpha(transaction),
            version = "DefaultFallbackHandler",
            address = address,
            label = R.string.tx_list_set_fallback_handler,
            nonce = transaction.nonce.toString()
        )
    }

    private fun queuedModuleChange(transaction: SettingsChange, threshold: Int): TransactionView.SettingsChangeVariantQueued {
        val thresholdMet = checkThreshold(threshold, transaction.confirmations)
        val address = getAddress(transaction, "module")
        val label = if (transaction.dataDecoded.method == "enableModule") R.string.tx_list_enable_module else R.string.tx_list_disable_module

        return TransactionView.SettingsChangeVariantQueued(
            id = transaction.id,
            status = transaction.status,
            statusText = displayString(transaction.status),
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

    private fun queuedMastercopyChange(transaction: SettingsChange, threshold: Int): TransactionView.SettingsChangeVariantQueued {
        val thresholdMet = checkThreshold(threshold, transaction.confirmations)
        val address = getAddress(transaction, "_masterCopy")
        val version = masterCopyVersion(address) ?: SAFE_MASTER_COPY_UNKNOWN_DISPLAY_STRING

        return TransactionView.SettingsChangeVariantQueued(
            id = transaction.id,
            status = transaction.status,
            statusText = displayString(transaction.status),
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
        transaction.dataDecoded.parameters.getValueByName(key)?.asEthereumAddress()

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
            amountText = balanceFormatter.formatAmount(custom.value, isIncoming, ETH_SERVICE_TOKEN_INFO.decimals, ETH_SERVICE_TOKEN_INFO.symbol),
            amountColor = if (custom.value > BigInteger.ZERO && isIncoming) R.color.safe_green else R.color.gnosis_dark_blue,
            alpha = alpha(custom),
            nonce = custom.nonce?.toString() ?: ""
        )
    }

    private fun queuedCustomTransaction(custom: Custom, safeInfo: SafeInfo): TransactionView.CustomTransactionQueued {
        val isIncoming: Boolean = custom.address == safeInfo.address
        val thresholdMet = checkThreshold(safeInfo.threshold, custom.confirmations)
        return TransactionView.CustomTransactionQueued(
            id = custom.id,
            status = custom.status,
            statusText = displayString(custom.status),
            statusColorRes = statusTextColor(custom.status),
            dateTimeText = custom.date?.formatBackendDate() ?: "",
            address = custom.address,
            confirmations = custom.confirmations ?: 0,
            threshold = safeInfo.threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = custom.nonce?.toString() ?: "",
            dataSizeText = if (custom.dataSize >= 0) "${custom.dataSize} bytes" else "",
            amountText = balanceFormatter.formatAmount(custom.value, isIncoming, ETH_SERVICE_TOKEN_INFO.decimals, ETH_SERVICE_TOKEN_INFO.symbol),
            amountColor = if (custom.value > BigInteger.ZERO && isIncoming) R.color.safe_green else R.color.gnosis_dark_blue
        )
    }

    private fun historicCreation(transaction: Creation): TransactionView.Creation {

        val txInfo = transaction.txInfo as TransactionInfo.Creation

        return TransactionView.Creation(
            id = transaction.id,
            status = transaction.status,
            statusText = displayString(transaction.status),
            statusColorRes = statusTextColor(transaction.status),
            dateTimeText = transaction.timestamp.formatBackendDate(),
            label = R.string.tx_list_creation,
            creationDetails = TransactionView.CreationDetails(
                statusText = displayString(transaction.status),
                statusColorRes = statusTextColor(transaction.status),
                dateTimeText = transaction.timestamp.formatBackendDate(),
                creator = txInfo.creator.asEthereumAddressString(),
                factory = txInfo.factory?.asEthereumAddressString(),
                implementation = txInfo.implementation?.asEthereumAddressString(),
                transactionHash = txInfo.transactionHash
            )
        )
    }

    private fun formatTransferAmount(viewTransfer: Transfer, incoming: Boolean): String {
        val symbol = viewTransfer.tokenInfo?.symbol ?: ""
        return balanceFormatter.formatAmount(viewTransfer.value, incoming, viewTransfer.tokenInfo?.decimals ?: 0, symbol)
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
        when (transaction.status) {
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
