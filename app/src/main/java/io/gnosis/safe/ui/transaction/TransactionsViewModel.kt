package io.gnosis.safe.ui.transaction

import androidx.annotation.StringRes
import io.gnosis.data.models.Page
import io.gnosis.data.models.SafeInfo
import io.gnosis.data.models.Transaction
import io.gnosis.data.models.Transaction.*
import io.gnosis.data.models.TransactionStatus
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.R
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.transaction.TransactionView.CustomTransaction
import io.gnosis.safe.ui.transaction.TransactionView.CustomTransactionQueued
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
        transaction: Transfer,
        safeInfo: SafeInfo
    ) = TransactionView.Transfer(transaction, transaction.recipient == safeInfo.address)

    private fun queuedTransfer(transaction: Transfer, safeInfo: SafeInfo): TransactionView? {
        return TransactionView.TransferQueued(transaction, transaction.recipient == safeInfo.address, safeInfo.threshold)
    }


    private fun addSectionHeaders(transactions: List<TransactionView>): List<TransactionView> {
        val mutableList = transactions.toMutableList()

        val firstQueuedTransaction = mutableList.indexOfFirst { transactionView ->
            when (transactionView.transaction?.status) {
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
            when (transactionView.transaction?.status) {
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

sealed class TransactionView(open val transaction: Transaction?) {
    data class SectionHeader(override val transaction: Transaction? = null, @StringRes val title: Int) : TransactionView(transaction)
    data class ChangeMastercopy(override val transaction: Transaction.SettingsChange) : TransactionView(transaction)
    data class ChangeMastercopyQueued(override val transaction: Transaction.SettingsChange, val threshold: Int) : TransactionView(transaction)
    data class CustomTransaction(override val transaction: Custom) : TransactionView(transaction)
    data class CustomTransactionQueued(override val transaction: Custom, val threshold: Int) : TransactionView(transaction)
    data class SettingsChange(override val transaction: Transaction.SettingsChange) : TransactionView(transaction)
    data class SettingsChangeQueued(override val transaction: Transaction.SettingsChange, val threshold: Int) : TransactionView(transaction)
    data class Transfer(val transfer: Transaction.Transfer, val isIncoming: Boolean/*, @ColorRes val statusColor: Int */) : TransactionView(transfer)
    data class TransferQueued(val transfer: Transaction.Transfer, val isIncoming: Boolean, val threshold: Int) : TransactionView(transfer)
}

data class TransactionsViewState(
    override var viewAction: BaseStateViewModel.ViewAction?,
    val isLoading: Boolean
) : BaseStateViewModel.State

data class LoadTransactions(
    val newTransactions: List<TransactionView>
) : BaseStateViewModel.ViewAction
