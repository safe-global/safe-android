package pm.gnosis.heimdall.ui.transactions

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.TransactionDetails
import pm.gnosis.heimdall.data.repositories.TransactionRepository

abstract class ReceiptTransactionContract: ViewModel() {
    abstract fun loadTransactionDetails(id: String): Single<TransactionDetails>
    abstract fun observeTransactionStatus(id: String): Observable<TransactionRepository.PublishStatus>
    abstract fun loadChainHash(id: String): Single<String>
}
