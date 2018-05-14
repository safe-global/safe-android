package pm.gnosis.heimdall.ui.transactions

import android.arch.lifecycle.ViewModel
import android.graphics.Bitmap
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.data.repositories.models.FeeEstimate
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.Result

abstract class ViewTransactionContract : ViewModel() {
    abstract fun checkTransactionType(transaction: Transaction): Single<TransactionType>
    abstract fun loadExecuteInfo(safeAddress: Solidity.Address, transaction: SafeTransaction): Observable<Result<Info>>
    abstract fun submitTransaction(safeAddress: Solidity.Address, transaction: SafeTransaction): Single<Result<Solidity.Address>>
    abstract fun estimateTransaction(info: Info): Single<Result<FeeEstimate>>
    abstract fun signTransaction(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        sendViaPush: Boolean = false
    ): Single<Result<Pair<String, Bitmap?>>>

    abstract fun loadExecutableTransaction(safeAddress: Solidity.Address, transaction: SafeTransaction): Single<Transaction>
    abstract fun addLocalTransaction(safeAddress: Solidity.Address, transaction: SafeTransaction, txChainHash: String): Single<String>
    abstract fun observeSignaturePushes(safeAddress: Solidity.Address, transaction: SafeTransaction): Observable<Result<Unit>>
    abstract fun sendSignaturePush(info: Info): Single<Result<Unit>>
    abstract fun addSignature(encodedSignatureUrl: String): Completable

    data class Info(
        val selectedSafe: Solidity.Address,
        val status: TransactionRepository.ExecuteInformation,
        val signatures: Map<Solidity.Address, Signature>
    )
}
