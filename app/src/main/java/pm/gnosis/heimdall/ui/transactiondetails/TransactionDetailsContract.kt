package pm.gnosis.heimdall.ui.transactiondetails

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.data.contracts.GnosisMultisigTransaction
import pm.gnosis.heimdall.data.model.TransactionDetails
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import pm.gnosis.heimdall.data.repositories.model.MultisigWallet
import java.math.BigInteger

abstract class TransactionDetailsContract : ViewModel() {
    abstract fun setTransaction(transactionDetails: TransactionDetails?): Completable
    abstract fun getTransaction(): TransactionDetails
    abstract fun getMultisigTransactionId(): BigInteger
    abstract fun getMultisigTransactionType(): MultisigTransactionType

    abstract fun getMultisigWalletDetails(): Flowable<MultisigWallet>
    abstract fun signTransaction(): Observable<Result<String>>
    abstract fun addMultisigWallet(address: String, name: String = ""): Single<Result<String>>
    abstract fun getTransactionDetails(): Observable<GnosisMultisigTransaction>
    abstract fun getTokenInfo(address: BigInteger): Observable<ERC20Token>
}

sealed class MultisigTransactionType
class ConfirmMultisigTransaction : MultisigTransactionType()
class RevokeMultisigTransaction : MultisigTransactionType()
