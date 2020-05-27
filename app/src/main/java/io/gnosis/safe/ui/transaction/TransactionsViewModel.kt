package io.gnosis.safe.ui.transaction

import io.gnosis.data.models.TransactionDto
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
                    viewAction = LoadTransactions(transactions.map { TransactionView.Transfer(it) })
                )
            }
        }
    }
}

sealed class TransactionView(open val transactionDto: TransactionDto) {

    data class ChangeMastercopy(override val transactionDto: TransactionDto) : TransactionView(transactionDto)
    data class ChangeMastercopyQueued(override val transactionDto: TransactionDto) : TransactionView(transactionDto)
    data class SettingsChange(override val transactionDto: TransactionDto) : TransactionView(transactionDto)
    data class SettingsChangeQueued(override val transactionDto: TransactionDto) : TransactionView(transactionDto)
    data class Transfer(override val transactionDto: TransactionDto) : TransactionView(transactionDto)
    data class TransferQueued(override val transactionDto: TransactionDto) : TransactionView(transactionDto)
}

data class TransactionsViewState(
    override var viewAction: BaseStateViewModel.ViewAction?,
    val isLoading: Boolean
) : BaseStateViewModel.State

data class LoadTransactions(
    val newTransactions: List<TransactionView>
) : BaseStateViewModel.ViewAction
