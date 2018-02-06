package pm.gnosis.heimdall.ui.transactions

import android.arch.lifecycle.ViewModel
import android.graphics.Bitmap
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.accounts.base.models.Signature
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.data.repositories.models.GasEstimate
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import java.math.BigInteger


abstract class ViewTransactionContract : ViewModel() {
    abstract fun checkTransactionType(transaction: Transaction): Single<TransactionType>
    abstract fun loadExecuteInfo(safeAddress: BigInteger, transaction: Transaction): Observable<Result<Info>>
    abstract fun submitTransaction(safeAddress: BigInteger, transaction: Transaction, overrideGasPrice: Wei?): Single<Result<BigInteger>>
    abstract fun signTransaction(safeAddress: BigInteger, transaction: Transaction, sendViaPush: Boolean = false): Single<Result<Pair<String, Bitmap?>>>
    abstract fun observeSignaturePushes(safeAddress: BigInteger, transaction: Transaction): Observable<Result<Unit>>
    abstract fun sendSignaturePush(info: Info): Single<Result<Unit>>
    abstract fun addSignature(encodedSignatureUrl: String): Completable

    data class Info(val selectedSafe: BigInteger, val status: TransactionRepository.ExecuteInformation, val signatures: Map<BigInteger, Signature>, val estimate: GasEstimate? = null)
}
