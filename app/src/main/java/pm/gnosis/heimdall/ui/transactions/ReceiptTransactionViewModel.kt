package pm.gnosis.heimdall.ui.transactions

import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.TransactionDetails
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import java.math.BigInteger
import javax.inject.Inject

class ReceiptTransactionViewModel @Inject constructor(
        private val safeTransactionsRepository: TransactionRepository,
        private val transactionDetailsRepository: TransactionDetailsRepository
) : ReceiptTransactionContract() {
    override fun loadTransactionDetails(id: String): Single<TransactionDetails> =
            transactionDetailsRepository.loadTransactionDetails(id)

    override fun observeTransactionStatus(id: String): Observable<TransactionRepository.PublishStatus> =
            safeTransactionsRepository.observePublishStatus(id)

    override fun loadChainHash(id: String): Single<String> =
            safeTransactionsRepository.loadChainHash(id)
}
