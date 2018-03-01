package pm.gnosis.heimdall.ui.transactions

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.helpers.SignatureStore
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.utils.GnoSafeUrlParser
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.di.ApplicationContext
import pm.gnosis.svalinn.common.utils.*
import pm.gnosis.utils.HttpCodes
import retrofit2.HttpException
import java.math.BigInteger
import javax.inject.Inject

class ViewTransactionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val qrCodeGenerator: QrCodeGenerator,
    private val signaturePushRepository: SignaturePushRepository,
    private val signatureStore: SignatureStore,
    private val transactionRepository: TransactionRepository,
    private val transactionDetailsRepository: TransactionDetailsRepository,
    private val txExecutorRepository: TxExecutorRepository
) : ViewTransactionContract() {

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context)
        .build()

    override fun observeHasCredits() =
        txExecutorRepository.observePlan()

    override fun buyCredit(activity: Activity) =
        txExecutorRepository.buyPlan(activity)

    override fun checkTransactionType(transaction: Transaction): Single<TransactionType> {
        return transactionDetailsRepository.loadTransactionType(transaction)
    }

    private fun checkSignature(safe: BigInteger, transaction: Transaction, signature: Signature) =
        transactionRepository.checkSignature(safe, transaction, signature)
            .onErrorResumeNext { Single.error(SimpleLocalizedException(context.getString(R.string.invalid_signature))) }

    override fun addLocalTransaction(safeAddress: BigInteger, transaction: Transaction, txChainHash: String): Single<String> =
        transactionRepository.addLocalTransaction(safeAddress, transaction, txChainHash)

    override fun observeSignaturePushes(safeAddress: BigInteger, transaction: Transaction): Observable<Result<Unit>> =
        signaturePushRepository.observe(safeAddress)
            .flatMapSingle {
                checkSignature(safeAddress, transaction, it)
                    .map(signatureStore::add)
                    .mapToResult()
            }

    override fun sendSignaturePush(info: Info): Single<Result<Unit>> =
        Single.fromCallable {
            val transaction = info.status.transaction
            val transactionHash = info.status.transactionHash
            GnoSafeUrlParser.signRequest(
                transactionHash,
                info.selectedSafe,
                transaction.address,
                transaction.value,
                transaction.data,
                transaction.nonce!!
            )
        }
            .subscribeOn(Schedulers.computation())
            .flatMapCompletable {
                signaturePushRepository.request(info.selectedSafe, it)
            }
            .mapToResult()

    override fun addSignature(encodedSignatureUrl: String): Completable {
        return signatureStore.loadSingingInfo()
            .flatMap { (safe, transaction) ->
                (GnoSafeUrlParser.parse(encodedSignatureUrl) as? GnoSafeUrlParser.Parsed.SignResponse)?.let {
                    checkSignature(safe, transaction, it.signature)
                }
                        ?: throw SimpleLocalizedException(context.getString(R.string.invalid_signature_uri))
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
                Observable.just<Result<Info>>(DataResult(Info(safeAddress, info, signatures)))
                /*
                TODO:
                Observable.concatDelayError(listOf(
                    Observable.just(DataResult(Info(safeAddress, info, signatures))),
                    info.checkMap(signatures)
                        .flatMapSingle { transactionRepository.estimateFees(safeAddress, info.transaction, signatures, info.isOwner) }
                        .map { Info(safeAddress, info, signatures, it) }
                        .onErrorResumeNext(Function { errorHandler.observable(it) })
                        .mapToResult()
                )
                )
                */
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
            .onErrorResumeNext({
                if (it is HttpException && it.code() == HttpCodes.UNAUTHORIZED)
                    Single.error(IllegalStateException())
                else
                    errorHandler.single(it)
            })
            .mapToResult()
    }

    override fun loadExecutableTransaction(safeAddress: BigInteger, transaction: Transaction): Single<Transaction> =
        transactionRepository.loadExecuteInformation(safeAddress, transaction)
            .flatMap { info ->
                // Observe local signature store
                signatureStore.load()
                    .map { info.check(it); it }
                    .flatMap { transactionRepository.loadExecutableTransaction(safeAddress, info.transaction, it, info.isOwner) }
            }

    override fun signTransaction(safeAddress: BigInteger, transaction: Transaction, sendViaPush: Boolean): Single<Result<Pair<String, Bitmap?>>> {
        return transactionRepository.sign(safeAddress, transaction)
            .flatMap { signature ->
                if (sendViaPush)
                    signaturePushRepository.send(safeAddress, transaction, signature)
                        .andThen(Single.just(signature.toString() to null))
                else
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
