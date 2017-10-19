package pm.gnosis.heimdall.ui.transactiondetails

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.contracts.GnosisMultisigTransaction
import pm.gnosis.heimdall.data.models.TransactionDetails
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.MultisigWallet
import java.math.BigInteger

abstract class TransactionDetailsContract : ViewModel() {
    abstract fun setTransaction(transactionDetails: TransactionDetails?): Completable
    abstract fun getTransaction(): TransactionDetails
    abstract fun getMultisigTransactionId(): BigInteger
    abstract fun getMultisigTransactionType(): MultisigTransactionType

    abstract fun observeMultisigWalletDetails(): Flowable<MultisigWallet>
    abstract fun signTransaction(): Observable<Result<String>>
    abstract fun addMultisigWallet(address: BigInteger, name: String?): Single<Result<BigInteger>>
    abstract fun loadTransactionDetails(): Observable<GnosisMultisigTransaction>
    abstract fun loadTokenInfo(address: BigInteger): Observable<ERC20Token>
}

sealed class MultisigTransactionType
class ConfirmMultisigTransaction : MultisigTransactionType()
class RevokeMultisigTransaction : MultisigTransactionType()
