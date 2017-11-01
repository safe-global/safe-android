package pm.gnosis.heimdall.ui.transactiondetails

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.impls.TransactionDetails
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.models.Transaction
import java.math.BigInteger

abstract class TransactionDetailsContract : ViewModel() {
    abstract fun setTransaction(transaction: Transaction?, descriptionHash: String?): Completable
    abstract fun getTransaction(): Transaction
    abstract fun getTransactionHash(): String
    abstract fun getTransactionType(): MultisigTransactionType

    abstract fun observeSafeDetails(): Flowable<Safe>
    abstract fun signTransaction(): Observable<Result<String>>
    abstract fun addSafe(address: BigInteger, name: String?): Single<Result<BigInteger>>
    abstract fun loadTransactionDetails(): Observable<TransactionDetails>
    abstract fun loadTokenInfo(address: BigInteger): Observable<ERC20Token>
}

sealed class MultisigTransactionType
class ConfirmMultisigTransaction : MultisigTransactionType()
class RevokeMultisigTransaction : MultisigTransactionType()
