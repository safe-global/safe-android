package io.gnosis.safe.ui.transaction

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.gnosis.data.models.Page
import io.gnosis.data.models.SafeInfo
import io.gnosis.data.models.Transaction
import io.gnosis.data.models.Transaction.*
import io.gnosis.data.models.TransactionStatus
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.SafeRepository.Companion.SAFE_MASTER_COPY_0_0_2
import io.gnosis.data.repositories.SafeRepository.Companion.SAFE_MASTER_COPY_0_1_0
import io.gnosis.data.repositories.SafeRepository.Companion.SAFE_MASTER_COPY_1_0_0
import io.gnosis.data.repositories.SafeRepository.Companion.SAFE_MASTER_COPY_1_1_1
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.data.repositories.getValueByName
import io.gnosis.safe.R
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.transaction.TransactionView.CustomTransaction
import io.gnosis.safe.ui.transaction.TransactionView.CustomTransactionQueued
import io.gnosis.safe.utils.shiftedString
import kotlinx.coroutines.flow.collect
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import javax.inject.Inject

class TransactionsViewModel
@Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TransactionsViewState>(appDispatchers) {

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { load() }
        }
    }

    override fun initialState(): TransactionsViewState = TransactionsViewState(null, true)

    fun load() {
        safeLaunch {
            val safeAddress = safeRepository.getActiveSafe()?.address
            if (safeAddress != null) {
                loadTransactions(safeAddress)
            } else {
                updateState(forceViewAction = true) { TransactionsViewState(isLoading = false, viewAction = ViewAction.ShowEmptyState) }
            }
        }
    }

    private suspend fun loadTransactions(safe: Solidity.Address) {
        val safeInfo = safeRepository.getSafeInfo(safe)
        val transactions = transactionRepository.getTransactions(safe, safeInfo)
        updateState {
            val mappedTransactions = mapTransactions(transactions, safeInfo)
            val transactionsWithSectionHeaders = addSectionHeaders(mappedTransactions)
            TransactionsViewState(
                isLoading = false,
                viewAction = mappedTransactions
                    .takeUnless { it.isEmpty() }
                    ?.let { LoadTransactions(transactionsWithSectionHeaders) }
                    ?: ViewAction.ShowEmptyState
            )
        }
    }

    private fun mapTransactions(
        transactions: Page<Transaction>,
        safeInfo: SafeInfo
    ): List<TransactionView> {
        return transactions.results.mapNotNull { transaction ->
            when {
                transaction is Transfer && isHistoricTransfer(transaction) -> historicTransfer(transaction, safeInfo)
                transaction is Transfer && isQueuedTransfer(transaction) -> queuedTransfer(transaction, safeInfo)
                transaction is SettingsChange && isQueuedMastercopyChange(transaction) -> queuedMastercopyChange(transaction, safeInfo.threshold)
                transaction is SettingsChange && isHistoricMastercopyChange(transaction) -> historyMastercopyChange(transaction)
                transaction is SettingsChange && isQueuedSettingsChange(transaction) -> queuedSettingsChange(transaction, safeInfo.threshold)
                transaction is SettingsChange && isHistoricSettingsChange(transaction) -> historicSettingsChange(transaction)
                transaction is Custom && isQueuedCustomTransaction(transaction) -> queuedCustomTransaction(transaction, safeInfo.threshold)
                transaction is Custom && isHistoricCustomTransaction(transaction) -> historicCustomTransaction(transaction)
                else -> null
            }
        }
    }

    private fun isHistoricMastercopyChange(settingsChange: SettingsChange): Boolean {
        return settingsChange.isChangeMasterCopy() && settingsChange.isCompleted()
    }

    private fun isQueuedMastercopyChange(settingsChange: SettingsChange): Boolean {
        return settingsChange.isChangeMasterCopy() && !settingsChange.isCompleted()
    }

    private fun isHistoricCustomTransaction(custom: Custom): Boolean {
        return custom.isCompleted()
    }

    private fun isQueuedCustomTransaction(custom: Custom): Boolean {
        return !custom.isCompleted()
    }

    private fun isHistoricSettingsChange(settingsChange: SettingsChange): Boolean {
        return !settingsChange.isChangeMasterCopy() && settingsChange.isCompleted()
    }

    private fun isQueuedSettingsChange(settingsChange: SettingsChange): Boolean {
        return !settingsChange.isChangeMasterCopy() && !settingsChange.isCompleted()
    }

    private fun isHistoricTransfer(transfer: Transfer): Boolean {
        return transfer.isCompleted()
    }

    private fun isQueuedTransfer(transfer: Transfer): Boolean {
        return !transfer.isCompleted()
    }

    private fun queuedSettingsChange(transaction: SettingsChange, threshold: Int): TransactionView.SettingsChangeQueued {
        val thresholdMet: Boolean = transaction.confirmations?.let {
            it >= threshold
        } ?: false

        return TransactionView.SettingsChangeQueued(
            status = transaction.status,
            statusText = transaction.status.displayString,
            statusColorRes = statusTextColor(transaction.status),
            dateTimeText = transaction.date ?: "",
            settingNameText = transaction.dataDecoded.method,
            confirmations = transaction.confirmations ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = transaction.nonce.toString()
        )
    }

    private fun historicCustomTransaction(custom: Custom): CustomTransaction =
        CustomTransaction(
            status = custom.status,
            statusText = custom.status.displayString,
            statusColorRes = statusTextColor(custom.status),
            dateTimeText = custom.date ?: "",
            address = custom.address,
            dataSizeText = if (custom.dataSize > 0) "${custom.dataSize} bytes" else "",
            amountText = "${custom.value} ETH",
            amountColor = R.color.gnosis_dark_blue,
            alpha = alpha(custom)
            //TODO: ETH value formatting missing
        )

    private fun queuedCustomTransaction(custom: Custom, threshold: Int): CustomTransactionQueued {
        val thresholdMet: Boolean = custom.confirmations?.let {
            it >= threshold
        } ?: false

        return CustomTransactionQueued(
            status = custom.status,
            statusText = custom.status.displayString,
            statusColorRes = statusTextColor(custom.status),
            dateTimeText = custom.date ?: "",
            address = custom.address,
            confirmations = custom.confirmations ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = custom.nonce.toString(),
            dataSizeText = if (custom.dataSize > 0) "${custom.dataSize} bytes" else "",
            amountText = "${custom.value} ETH",
            amountColor = R.color.gnosis_dark_blue
            //TODO: ETH amount formatting missing

        )
    }

    private fun historicSettingsChange(transaction: SettingsChange): TransactionView.SettingsChange =
        TransactionView.SettingsChange(
            status = transaction.status,
            statusText = transaction.status.displayString,
            statusColorRes = statusTextColor(transaction.status),
            dateTimeText = transaction.date ?: "",
            settingNameText = transaction.dataDecoded.method,
            alpha = alpha(transaction)
        )

    private fun queuedMastercopyChange(transaction: SettingsChange, threshold: Int): TransactionView.ChangeMastercopyQueued {
        val thresholdMet: Boolean = transaction.confirmations?.let {
            it >= threshold
        } ?: false

        return TransactionView.ChangeMastercopyQueued(
            status = transaction.status,
            statusText = transaction.status.displayString,
            statusColorRes = statusTextColor(transaction.status),
            dateTimeText = transaction.date ?: "",
            confirmations = transaction.confirmations ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = transaction.nonce.toString()
        )
    }

    private fun historyMastercopyChange(transaction: SettingsChange): TransactionView.ChangeMastercopy {

        val address = transaction.dataDecoded.parameters.getValueByName("_masterCopy")?.asEthereumAddress()
        val version = when (address) {
            SAFE_MASTER_COPY_0_0_2 -> "0.0.2"
            SAFE_MASTER_COPY_0_1_0 -> "0.1.0"
            SAFE_MASTER_COPY_1_0_0 -> "1.0.0"
            SAFE_MASTER_COPY_1_1_1 -> "1.1.1"
            else -> "unknown"
        }

        return TransactionView.ChangeMastercopy(
            status = transaction.status,
            statusText = transaction.status.displayString,
            statusColorRes = statusTextColor(transaction.status),
            dateTimeText = transaction.date ?: "",
            alpha = alpha(transaction),
            version = version,
            address = address
        )
    }

    private fun historicTransfer(
        transfer: Transfer,
        safeInfo: SafeInfo
    ): TransactionView.Transfer {
        val isIncoming: Boolean = transfer.recipient == safeInfo.address

        return TransactionView.Transfer(
            status = transfer.status,
            statusText = transfer.status.displayString,
            statusColorRes = statusTextColor(transfer.status),
            amountText = formatAmount(transfer, isIncoming),
            dateTimeText = transfer.date ?: "",
            txTypeIcon = if (isIncoming) R.drawable.ic_arrow_green_16dp else R.drawable.ic_arrow_red_10dp,
            address = if (isIncoming) transfer.sender else transfer.recipient,
            amountColor = if (isIncoming) R.color.safe_green else R.color.gnosis_dark_blue,
            alpha = alpha(transfer)
        )
    }

    private fun queuedTransfer(transfer: Transfer, safeInfo: SafeInfo): TransactionView? {
        val thresholdMet: Boolean = transfer.confirmations?.let {
            it >= safeInfo.threshold
        } ?: false
        val isIncoming: Boolean = transfer.recipient == safeInfo.address

        return TransactionView.TransferQueued(
            status = transfer.status,
            statusText = transfer.status.displayString,
            statusColorRes = statusTextColor(transfer.status),
            amountText = formatAmount(transfer, isIncoming),
            dateTimeText = transfer.date ?: "",
            txTypeIcon = if (isIncoming) R.drawable.ic_arrow_green_16dp else R.drawable.ic_arrow_red_10dp,
            address = if (isIncoming) transfer.sender else transfer.recipient,
            amountColor = if (isIncoming) R.color.safe_green else R.color.gnosis_dark_blue,
            confirmations = transfer.confirmations ?: 0,
            threshold = safeInfo.threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = transfer.nonce.toString()
        )
    }

    private fun formatAmount(viewTransfer: Transaction.Transfer, incoming: Boolean): String {
        val inOut = if (incoming) "+" else "-"
        val symbol = viewTransfer.tokenInfo?.symbol
        val value: String = viewTransfer.tokenInfo?.decimals?.let { viewTransfer.value.shiftedString(decimals = it) }.toString()
        return "%s%s %s".format(inOut, value, symbol)
    }

    private fun statusTextColor(status: TransactionStatus): Int {
        return when (status) {
            TransactionStatus.Success -> R.color.safe_green
            TransactionStatus.Cancelled -> R.color.dark_grey
            else -> R.color.safe_failed_red
        }
    }

    private fun alpha(transaction: Transaction): Float =
        when (transaction.status) {
            TransactionStatus.Failed, TransactionStatus.Cancelled -> 0.5F
            else -> 1.0F
        }

    private fun addSectionHeaders(transactions: List<TransactionView>): List<TransactionView> {
        val mutableList = transactions.toMutableList()

        val firstQueuedTransaction = mutableList.indexOfFirst { transactionView ->
            when (transactionView.status) {
                TransactionStatus.Pending,
                TransactionStatus.AwaitingConfirmations,
                TransactionStatus.AwaitingExecution -> true
                else -> false
            }
        }
        if (firstQueuedTransaction >= 0) {
            mutableList.add(firstQueuedTransaction, TransactionView.SectionHeader(title = R.string.tx_list_queue))
        }

        val firstHistoricTransaction = mutableList.indexOfFirst { transactionView ->
            when (transactionView.status) {
                TransactionStatus.Cancelled,
                TransactionStatus.Failed,
                TransactionStatus.Success -> true
                else -> false
            }
        }

        if (firstHistoricTransaction >= 0) {
            mutableList.add(firstHistoricTransaction, TransactionView.SectionHeader(title = R.string.tx_list_history))
        }
        return mutableList
    }
}

sealed class TransactionView(open val status: TransactionStatus) {
    data class Transfer(
        override val status: TransactionStatus,
        val statusText: String,
        @ColorRes val statusColorRes: Int,
        val amountText: String,
        val dateTimeText: String,
        @DrawableRes val txTypeIcon: Int,
        val address: Solidity.Address,
        @ColorRes val amountColor: Int,
        val alpha: Float
    ) : TransactionView(status)

    data class TransferQueued(
        override val status: TransactionStatus,
        val statusText: String,
        @ColorRes val statusColorRes: Int,
        val amountText: String,
        val dateTimeText: String,
        @DrawableRes val txTypeIcon: Int,
        val address: Solidity.Address,
        @ColorRes val amountColor: Int,
        val confirmations: Int,
        val threshold: Int,
        @ColorRes val confirmationsTextColor: Int,
        @DrawableRes val confirmationsIcon: Int,
        val nonce: String
    ) : TransactionView(status)

    data class SettingsChange(
        override val status: TransactionStatus,
        val statusText: String,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val settingNameText: String,
        val alpha: Float
    ) : TransactionView(status)

    data class SettingsChangeQueued(
        override val status: TransactionStatus,
        val statusText: String,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val settingNameText: String,
        val confirmations: Int,
        val threshold: Int,
        @ColorRes val confirmationsTextColor: Int,
        @DrawableRes val confirmationsIcon: Int,
        val nonce: String
    ) : TransactionView(status)

    data class ChangeMastercopy(
        override val status: TransactionStatus,
        val statusText: String,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val address: Solidity.Address?,
        val version: String,
        val alpha: Float
    ) : TransactionView(status)

    data class ChangeMastercopyQueued(
        override val status: TransactionStatus,
        val statusText: String,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val confirmations: Int,
        val threshold: Int,
        @ColorRes val confirmationsTextColor: Int,
        @DrawableRes val confirmationsIcon: Int,
        val nonce: String
    ) : TransactionView(status)

    data class CustomTransaction(
        override val status: TransactionStatus,
        val statusText: String,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val address: Solidity.Address,
        val dataSizeText: String,
        val amountText: String,
        @ColorRes val amountColor: Int,
        val alpha: Float
    ) : TransactionView(status)

    data class CustomTransactionQueued(
        override val status: TransactionStatus,
        val statusText: String,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val address: Solidity.Address,
        val confirmations: Int,
        val threshold: Int,
        @ColorRes val confirmationsTextColor: Int,
        @DrawableRes val confirmationsIcon: Int,
        val dataSizeText: String,
        val amountText: String,
        @ColorRes val amountColor: Int,
        val nonce: String
    ) : TransactionView(status)

    data class SectionHeader(@StringRes val title: Int) : TransactionView(TransactionStatus.Pending)
}

data class TransactionsViewState(
    override var viewAction: BaseStateViewModel.ViewAction?,
    val isLoading: Boolean
) : BaseStateViewModel.State

data class LoadTransactions(
    val newTransactions: List<TransactionView>
) : BaseStateViewModel.ViewAction
