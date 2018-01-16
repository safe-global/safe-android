package pm.gnosis.heimdall.ui.transactions

import android.content.Context
import android.graphics.Bitmap
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Function
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.accounts.base.models.Signature
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.*
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.helpers.SignatureStore
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.utils.GnoSafeUrlParser
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import java.math.BigInteger
import javax.inject.Inject


class ViewTransactionViewModel @Inject constructor(
        @ApplicationContext private val context: Context,
        private var qrCodeGenerator: QrCodeGenerator,
        private val signatureStore: SignatureStore,
        private val transactionRepository: TransactionRepository,
        private val transactionDetailsRepository: TransactionDetailsRepository
) : ViewTransactionContract() {

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context)
            .build()

    override fun checkTransactionType(transaction: Transaction): Single<TransactionType> {
        return transactionDetailsRepository.loadTransactionType(transaction)
    }

    override fun addSignature(encodedSignatureUrl: String): Completable {
        return signatureStore.loadSingingInfo()
                .flatMap { (safe, transaction) ->
                    (GnoSafeUrlParser.parse(encodedSignatureUrl) as? GnoSafeUrlParser.Parsed.SignResponse)?.let {
                        transactionRepository.checkSignature(safe, transaction, it.signature)
                    } ?: throw SimpleLocalizedException(context.getString(R.string.invalid_signature_uri))
                }
                .map(signatureStore::add)
                .toCompletable()
    }

    override fun loadExecuteInfo(safeAddress: BigInteger, transaction: Transaction): Observable<Result<Info>> {
        return transactionRepository.loadExecuteInformation(safeAddress, transaction)
                .flatMapObservable { info ->
                    // Observe local signature store
                    signatureStore.flatMapInfo(safeAddress, info)
                            .onErrorReturnItem(emptyMap())
                            .map { info to it }
                }
                .flatMap { (info, signatures) ->
                    Observable.concatDelayError(listOf(
                            Observable.just(DataResult(Info(safeAddress, info, signatures))),
                            info.checkMap(signatures)
                                    .flatMapSingle { transactionRepository.estimateFees(safeAddress, info.transaction, signatures, info.isOwner) }
                                    .map { Info(safeAddress, info, signatures, it) }
                                    .onErrorResumeNext(Function { errorHandler.observable(it) })
                                    .mapToResult()
                    ))
                }
                .onErrorReturn { ErrorResult(it) }
    }

    override fun submitTransaction(safeAddress: BigInteger, transaction: Transaction, overrideGasPrice: Wei?): Single<Result<BigInteger>> {
        return transactionRepository.loadExecuteInformation(safeAddress, transaction)
                .flatMapCompletable { info ->
                    // Observe local signature store
                    signatureStore.load()
                            .map { info.check(it); it }
                            .flatMapCompletable { transactionRepository.submit(safeAddress, info.transaction, it, info.isOwner, overrideGasPrice) }
                }
                .andThen(Single.just(safeAddress))
                .onErrorResumeNext({ errorHandler.single(it) })
                .mapToResult()
    }

    override fun signTransaction(safeAddress: BigInteger, transaction: Transaction): Single<Result<Pair<String, Bitmap>>> {
        return transactionRepository.sign(safeAddress, transaction)
                .flatMap { signature ->
                    qrCodeGenerator
                            .generateQrCode(GnoSafeUrlParser.signResponse(signature))
                            .map { signature.toString() to it }
                }
                .onErrorResumeNext({ errorHandler.single(it) })
                .mapToResult()
    }

    private fun TransactionRepository.ExecuteInformation.check(signatures: Map<BigInteger, Signature>): TransactionRepository.ExecuteInformation =
            when {
                requiredConfirmation - signatures.size - (if (isOwner) 1 else 0) > 0 ->
                    throw SimpleLocalizedException(context.getString(R.string.error_not_enough_confirmations))
                else -> this
            }

    private fun TransactionRepository.ExecuteInformation.checkMap(signatures: Map<BigInteger, Signature>) =
            Observable.fromCallable { check(signatures) }.onErrorResumeNext(Function { Observable.empty<TransactionRepository.ExecuteInformation>() })

}
