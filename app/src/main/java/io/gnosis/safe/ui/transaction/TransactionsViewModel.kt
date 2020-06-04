package io.gnosis.safe.ui.transaction

import io.gnosis.data.models.Transaction
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject

class TransactionsViewModel
@Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TransactionsViewState>(appDispatchers) {

    override fun initialState(): TransactionsViewState = TransactionsViewState(null, true)

    fun load() {//safeAddress: Solidity.Address) {
        safeLaunch {
            val safeAddress = safeRepository.getActiveSafe()!!.address
            val transactions = transactionRepository.getTransactions(safeAddress)
            updateState {
                TransactionsViewState(
                    isLoading = false,
                    viewAction = LoadTransactions(transactions.results.mapNotNull { transaction ->
                        when (transaction) {
                            is Transaction.Transfer -> TransactionView.Transfer(transaction, transaction.recipient == safeAddress)
                            is Transaction.SettingsChange -> TransactionView.SettingsChange(transaction)
                            else -> null
                        }
                    })
                )
            }
        }
    }
}

sealed class TransactionView(open val transaction: Transaction) {

    data class ChangeMastercopy(override val transaction: Transaction) : TransactionView(transaction)
    data class ChangeMastercopyQueued(override val transaction: Transaction) : TransactionView(transaction)
    data class SettingsChange(override val transaction: Transaction.SettingsChange) : TransactionView(transaction)
    data class SettingsChangeQueued(override val transaction: Transaction) : TransactionView(transaction)
    data class Transfer(override val transaction: Transaction.Transfer, val isIncoming: Boolean) : TransactionView(transaction)
    data class TransferQueued(override val transaction: Transaction) : TransactionView(transaction)
}

data class TransactionsViewState(
    override var viewAction: BaseStateViewModel.ViewAction?,
    val isLoading: Boolean
) : BaseStateViewModel.State

data class LoadTransactions(
    val newTransactions: List<TransactionView>
) : BaseStateViewModel.ViewAction
