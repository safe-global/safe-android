package pm.gnosis.heimdall.ui.transactions

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.data.repositories.models.GasEstimate
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import java.math.BigInteger


abstract class ViewTransactionContract : ViewModel() {
    abstract fun checkTransactionType(transaction: Transaction): Single<TransactionType>
    abstract fun loadTransactionInfo(safeAddress: BigInteger, transaction: Transaction): Observable<Result<Info>>
    abstract fun submitTransaction(safeAddress: BigInteger, transaction: Transaction, overrideGasPrice: Wei?): Single<Result<BigInteger>>

    data class Info(val selectedSafe: BigInteger, val transaction: Transaction, val status: TransactionRepository.TransactionStatus, val estimate: GasEstimate? = null)
}