package pm.gnosis.heimdall.ui.transactions

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import java.math.BigInteger


abstract class BaseTransactionContract: ViewModel() {
    abstract fun loadTransactionInfo(safeAddress: BigInteger, transaction: Transaction): Observable<Result<Info>>
    abstract fun submitTransaction(safeAddress: BigInteger, transaction: Transaction): Single<Result<Unit>>

    data class Info(val transactionInfo: TransactionRepository.TransactionInfo, val estimation: Wei? = null)
}