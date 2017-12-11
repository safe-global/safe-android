package pm.gnosis.heimdall.ui.transactions

import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.models.Transaction
import javax.inject.Inject


class ViewTransactionViewModel @Inject constructor(
    private val transactionDetailsRepository: TransactionDetailsRepository
) : ViewTransactionContract() {
    override fun checkTransactionType(transaction: Transaction): Single<TransactionType> {
        return transactionDetailsRepository.loadTransactionType(transaction)
    }
}