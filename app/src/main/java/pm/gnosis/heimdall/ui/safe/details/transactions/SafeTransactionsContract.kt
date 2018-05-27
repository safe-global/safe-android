package pm.gnosis.heimdall.ui.safe.details.transactions

import android.arch.lifecycle.ViewModel
import android.content.Intent
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionInfo
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result

abstract class SafeTransactionsContract : ViewModel() {
    abstract fun setup(address: Solidity.Address)

    abstract fun observeTransactions(): Flowable<out Result<Adapter.Data<String>>>

    abstract fun loadTransactionInfo(id: String): Single<Pair<TransactionInfo, TransferInfo?>>

    abstract fun observeTransactionStatus(id: String): Observable<TransactionExecutionRepository.PublishStatus>

    abstract fun transactionSelected(id: String): Single<Intent>

    data class TransferInfo(val amount: String, val symbol: String?)
}
