package io.gnosis.safe.ui.transaction

import androidx.annotation.StringRes
import io.gnosis.data.models.Page
import io.gnosis.data.models.Transaction
import io.gnosis.data.models.TransactionStatus
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.R
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
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
                updateState { TransactionsViewState(null, isLoading = true) }
                loadTransactions(safeAddress)
            } else {
                updateState(forceViewAction = true) { TransactionsViewState(isLoading = false, viewAction = NoSafeSelected) }
            }
        }
    }

    private suspend fun loadTransactions(safe: Solidity.Address) {
        val safeInfo = safeRepository.getSafeInfo(safe)
        val transactions = transactionRepository.getTransactions(safe, safeInfo)
        updateState {
            val mappedTransactions = mapTransactions(transactions, safe)
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
        safe: Solidity.Address
    ): List<TransactionView> {
        return transactions.results.mapNotNull { transaction ->
            when (transaction) {
                is Transaction.Transfer -> TransactionView.Transfer(transaction, transaction.recipient == safe)
                is Transaction.SettingsChange -> TransactionView.SettingsChange(transaction)
                else -> null
            }
        }
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
    data class ChangeMastercopy(override val transaction: Transaction) : TransactionView(transaction)
    data class ChangeMastercopyQueued(override val transaction: Transaction) : TransactionView(transaction)
    data class SettingsChange(override val transaction: Transaction.SettingsChange) : TransactionView(transaction)
    data class SettingsChangeQueued(override val transaction: Transaction) : TransactionView(transaction)
    data class Transfer(val transfer: Transaction.Transfer, val isIncoming: Boolean) : TransactionView(transfer)
    data class TransferQueued(override val transaction: Transaction) : TransactionView(transaction)
}

data class TransactionsViewState(
    override var viewAction: BaseStateViewModel.ViewAction?,
    val isLoading: Boolean
) : BaseStateViewModel.State

data class LoadTransactions(
    val newTransactions: List<TransactionView>
) : BaseStateViewModel.ViewAction

object NoSafeSelected : BaseStateViewModel.ViewAction
