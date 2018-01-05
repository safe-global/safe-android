package pm.gnosis.heimdall.ui.transactions

import android.content.Context
import android.util.Log
import io.reactivex.*
import io.reactivex.functions.Function
import io.reactivex.internal.operators.single.SingleError
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.accounts.base.models.Signature
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import java.math.BigInteger
import javax.inject.Inject


class ViewTransactionViewModel @Inject constructor(
        @ApplicationContext private val context: Context,
        private val signatureStore: SignatureStore,
        private val transactionRepository: TransactionRepository,
        private val transactionDetailsRepository: TransactionDetailsRepository
) : ViewTransactionContract() {

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context)
            .build()

    override fun checkTransactionType(transaction: Transaction): Single<TransactionType> {
        return transactionDetailsRepository.loadTransactionType(transaction)
    }

    override fun addSignature(encodedSignature: String): Completable {
        return signatureStore.loadInfo()
                .flatMap {
                    transactionRepository.parseSignature(it.selectedSafe, it.transaction, encodedSignature)
                }
                .map(signatureStore::addSignature)
                .toCompletable()
    }

    override fun loadExecuteInfo(safeAddress: BigInteger, transaction: Transaction): Observable<Result<Info>> {
        return transactionRepository.loadExecuteInformation(safeAddress, transaction)
                .flatMapObservable { info ->
                    // Observe local signature store
                    signatureStore.flatMapInfo(safeAddress, transaction, info).map { info to it }
                }
                .flatMap { (info, signatures) ->
                    Observable.concatDelayError(listOf(
                            Observable.just(DataResult(Info(safeAddress, transaction, info, signatures))),
                            info.checkMap(signatures)
                                    .flatMapSingle { transactionRepository.estimateFees(safeAddress, transaction.updateTransactionWithStatus(it), signatures) }
                                    .map { Info(safeAddress, transaction, info, signatures, it) }
                                    .onErrorResumeNext(Function { errorHandler.observable(it) })
                                    .mapToResult()
                    ))
                }
    }

    override fun submitTransaction(safeAddress: BigInteger, transaction: Transaction, overrideGasPrice: Wei?): Single<Result<BigInteger>> {
        return transactionRepository.loadExecuteInformation(safeAddress, transaction)
                .flatMapCompletable { info ->
                    // Observe local signature store
                    Single.create(signatureStore)
                            .map { info.check(it); it }
                            .flatMapCompletable { transactionRepository.submit(safeAddress, transaction.updateTransactionWithStatus(info), it, overrideGasPrice) }
                }
                .andThen(Single.just(safeAddress))
                .onErrorResumeNext({ errorHandler.single(it) })
                .mapToResult()
    }

    override fun signTransaction(safeAddress: BigInteger, transaction: Transaction): Single<Result<String>> {
        return transactionRepository.sign(safeAddress, transaction)
                .map { it.toString() }
                .onErrorResumeNext({ errorHandler.single(it) })
                .mapToResult()
    }

    private fun Transaction.updateTransactionWithStatus(status: TransactionRepository.ExecuteInformation) =
            nonce?.let { this } ?: copy(nonce = status.safeNonce)

    private fun TransactionRepository.ExecuteInformation.check(signatures: Map<BigInteger, Signature>): TransactionRepository.ExecuteInformation =
            when {
                requiredConfirmation - signatures.size - (if (isOwner) 1 else 0) > 0 ->
                    throw SimpleLocalizedException(context.getString(R.string.error_not_enough_confirmations))
                else -> this
            }

    private fun TransactionRepository.ExecuteInformation.checkMap(signatures: Map<BigInteger, Signature>) =
            Observable.fromCallable { check(signatures) }.onErrorResumeNext(Function { Observable.empty<TransactionRepository.ExecuteInformation>() })

    class SignatureStore @Inject constructor() : ObservableOnSubscribe<Map<BigInteger, Signature>>, SingleOnSubscribe<Map<BigInteger, Signature>> {

        private val signatureLock = Any()
        private val signatures = HashMap<BigInteger, Signature>()

        private var emitter: ObservableEmitter<Map<BigInteger, Signature>>? = null
        private var safeAddress: BigInteger? = null
        private var transaction: Transaction? = null
        private var info: TransactionRepository.ExecuteInformation? = null
            private set(value) {
                // Transaction changed, clear signatures
                if (field?.transactionHash != value?.transactionHash) {
                    signatures.clear()
                }
                field = value
            }

        override fun subscribe(e: ObservableEmitter<Map<BigInteger, Signature>>) {
            emitter = e
            // We only emit a copy of the signatures
            e.onNext(HashMap(signatures))
        }

        override fun subscribe(e: SingleEmitter<Map<BigInteger, Signature>>) {
            // We only emit a copy of the signatures
            e.onSuccess(HashMap(signatures))
        }

        fun flatMapInfo(safeAddress: BigInteger, transaction: Transaction, info: TransactionRepository.ExecuteInformation): Observable<Map<BigInteger, Signature>> {
            synchronized(signatureLock) {
                this.safeAddress = safeAddress
                this.transaction = transaction
                this.info = info
                // Check if any signatures are from owners that are not present anymore
                val validSignatures = signatures.filter { info.owners.contains(it.key) }
                signatures.clear()
                signatures.putAll(validSignatures)
            }
            // We only emit a copy of the signatures
            emitter?.onNext(HashMap(signatures))
            return Observable.create(this)
        }

        fun loadInfo(): Single<Info> =
                info?.let { Single.just(Info(safeAddress!!, transaction!!, it, signatures)) } ?: Single.error(IllegalStateException())

        fun addSignature(signature: Pair<BigInteger, Signature>) {
            synchronized(signatureLock) {
                if (info?.owners?.contains(signature.first) == false) throw IllegalArgumentException() // Not owner of this safe
                if (signatures.containsKey(signature.first)) throw IllegalArgumentException() // Already signed
                signatures.put(signature.first, signature.second)
            }
            // We only emit a copy of the signatures
            emitter?.onNext(HashMap(signatures))
        }
    }
}
