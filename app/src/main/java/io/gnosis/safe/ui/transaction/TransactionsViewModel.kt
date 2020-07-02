package io.gnosis.safe.ui.transaction

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.gnosis.data.models.Page
import io.gnosis.data.models.SafeInfo
import io.gnosis.data.models.Transaction
import io.gnosis.data.models.Transaction.Custom
import io.gnosis.data.models.Transaction.SettingsChange
import io.gnosis.data.models.Transaction.Transfer
import io.gnosis.data.models.TransactionStatus
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.R
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.transaction.TransactionView.CustomTransaction
import io.gnosis.safe.ui.transaction.TransactionView.CustomTransactionQueued
import io.gnosis.safe.utils.shiftedString
import kotlinx.coroutines.flow.collect
import pm.gnosis.model.Solidity
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
        return TransactionView.SettingsChangeQueued(transaction, threshold)
    }

    private fun historicCustomTransaction(custom: Custom): CustomTransaction {
        return CustomTransaction(custom)
    }

    private fun queuedCustomTransaction(custom: Custom, threshold: Int): CustomTransactionQueued {
        return CustomTransactionQueued(custom, threshold)
    }

    private fun historicSettingsChange(transaction: SettingsChange): TransactionView.SettingsChange {
        return TransactionView.SettingsChange(transaction)
    }

    private fun queuedMastercopyChange(transaction: SettingsChange, threshold: Int): TransactionView? {
        TODO("Not yet implemented")
    }

    private fun historyMastercopyChange(transaction: SettingsChange): TransactionView? {
        TODO("Not yet implemented")
    }

    private fun historicTransfer(
        transfer: Transfer,
        safeInfo: SafeInfo
    ): TransactionView.Transfer =
        TransactionView.Transfer(
            transfer.status,
            transfer.status.name,
            finaleStatusTextColor(transfer.status),
            formatAmount(transfer, isIncoming(transfer, safeInfo)),
            transfer.date ?: "",
            if (isIncoming(transfer, safeInfo)) R.drawable.ic_arrow_green_16dp else R.drawable.ic_arrow_red_10dp,
            transfer.sender,
            if (isIncoming(transfer, safeInfo)) R.color.safe_green else R.color.gnosis_dark_blue
        )

    private fun formatAmount(viewTransfer: Transaction.Transfer, incoming: Boolean): String {
        val inOut = if (incoming) "+" else "-"
        val symbol = viewTransfer.tokenInfo?.symbol
        val value: String = viewTransfer.tokenInfo?.decimals?.let { viewTransfer.value.shiftedString(decimals = it) }.toString()
        return "%s%s %s".format(inOut, value, symbol)
    }

    private fun finaleStatusTextColor(status: TransactionStatus): Int {
        return when (status) {
            TransactionStatus.Success -> R.color.safe_green
            TransactionStatus.Cancelled -> R.color.dark_grey
            else -> R.color.safe_failed_red
        }
    }

    private fun queuedTransfer(transaction: Transfer, safeInfo: SafeInfo): TransactionView? {
        return TransactionView.TransferQueued(transaction, isIncoming(transaction, safeInfo), safeInfo.threshold)
    }

    private fun isIncoming(transfer: Transfer, safeInfo: SafeInfo) = transfer.recipient == safeInfo.address

    private fun addSectionHeaders(transactions: List<TransactionView>): List<TransactionView> {
        val mutableList = transactions.toMutableList()

        val firstQueuedTransaction = mutableList.indexOfFirst { transactionView ->
            when (transactionView.status) {
                TransactionStatus.Pending,
                TransactionStatus.AwaitingConfirmation,
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
        @ColorRes val amountColor: Int
    ) : TransactionView(status)

    data class TransferQueued(val transfer: Transaction.Transfer, val isIncoming: Boolean, val threshold: Int) : TransactionView(transfer.status)
    data class SettingsChange(val transaction: Transaction.SettingsChange) : TransactionView(transaction.status)
    data class SettingsChangeQueued(val transaction: Transaction.SettingsChange, val threshold: Int) : TransactionView(transaction.status)
    data class ChangeMastercopy(val transaction: Transaction.SettingsChange) : TransactionView(transaction.status)
    data class ChangeMastercopyQueued(val transaction: Transaction.SettingsChange, val threshold: Int) : TransactionView(transaction.status)
    data class CustomTransaction(val transaction: Custom) : TransactionView(transaction.status)
    data class CustomTransactionQueued(val transaction: Custom, val threshold: Int) : TransactionView(transaction.status)
    data class SectionHeader(val transaction: Transaction? = null, @StringRes val title: Int) : TransactionView(TransactionStatus.Pending)
}

data class TransactionsViewState(
    override var viewAction: BaseStateViewModel.ViewAction?,
    val isLoading: Boolean
) : BaseStateViewModel.State

data class LoadTransactions(
    val newTransactions: List<TransactionView>
) : BaseStateViewModel.ViewAction
