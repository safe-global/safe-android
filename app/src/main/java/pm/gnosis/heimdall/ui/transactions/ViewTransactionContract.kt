package pm.gnosis.heimdall.ui.transactions

import android.arch.lifecycle.ViewModel
import android.graphics.Bitmap
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.data.repositories.models.FeeEstimate
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.Result
import java.math.BigInteger

abstract class ViewTransactionContract : ViewModel() {
    abstract fun checkTransactionType(transaction: Transaction): Single<TransactionType>
    abstract fun loadExecuteInfo(safeAddress: BigInteger, transaction: Transaction): Observable<Result<Info>>
    abstract fun submitTransaction(safeAddress: BigInteger, transaction: Transaction): Single<Result<BigInteger>>
    abstract fun estimateTransaction(info: Info): Single<Result<FeeEstimate>>
    abstract fun signTransaction(
        safeAddress: BigInteger,
        transaction: Transaction,
        sendViaPush: Boolean = false
    ): Single<Result<Pair<String, Bitmap?>>>

    abstract fun loadExecutableTransaction(safeAddress: BigInteger, transaction: Transaction): Single<Transaction>
    abstract fun addLocalTransaction(safeAddress: BigInteger, transaction: Transaction, txChainHash: String): Single<String>
    abstract fun observeSignaturePushes(safeAddress: BigInteger, transaction: Transaction): Observable<Result<Unit>>
    abstract fun sendSignaturePush(info: Info): Single<Result<Unit>>
    abstract fun addSignature(encodedSignatureUrl: String): Completable

    data class Info(
        val selectedSafe: BigInteger,
        val status: TransactionRepository.ExecuteInformation,
        val signatures: Map<BigInteger, Signature>
    )
}
