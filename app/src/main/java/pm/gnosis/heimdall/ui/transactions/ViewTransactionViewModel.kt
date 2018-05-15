package pm.gnosis.heimdall.ui.transactions

import android.content.Context
import android.graphics.Bitmap
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.SignaturePushRepository
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.data.repositories.models.FeeEstimate
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.helpers.SignatureStore
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.utils.GnoSafeUrlParser
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.*
import pm.gnosis.utils.HttpCodes
import retrofit2.HttpException
import javax.inject.Inject

class ViewTransactionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val qrCodeGenerator: QrCodeGenerator,
    private val signaturePushRepository: SignaturePushRepository,
    private val signatureStore: SignatureStore,
    private val transactionRepository: TransactionRepository,
    private val transactionDetailsRepository: TransactionDetailsRepository
) : ViewTransactionContract() {

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context).build()

    override fun checkTransactionType(transaction: Transaction): Single<TransactionType> =
        transactionDetailsRepository.loadTransactionType(transaction)

    private fun checkSignature(safe: Solidity.Address, transaction: SafeTransaction, signature: Signature) =
        transactionRepository.checkSignature(safe, transaction, signature)
            .onErrorResumeNext { Single.error(SimpleLocalizedException(context.getString(R.string.invalid_signature))) }

    override fun addLocalTransaction(safeAddress: Solidity.Address, transaction: SafeTransaction, txChainHash: String): Single<String> =
        transactionRepository.addLocalTransaction(safeAddress, transaction, txChainHash)

    override fun observeSignaturePushes(safeAddress: Solidity.Address, transaction: SafeTransaction): Observable<Result<Unit>> =
        signaturePushRepository.observe(safeAddress)
            .flatMapSingle {
                checkSignature(safeAddress, transaction, it)
                    .map(signatureStore::add)
                    .mapToResult()
            }

    override fun sendSignaturePush(info: Info): Single<Result<Unit>> =
        Single.fromCallable {
            val transaction = info.status.transaction.wrapped
            val transactionHash = info.status.transactionHash
            GnoSafeUrlParser.signRequest(
                transactionHash,
                info.selectedSafe,
                transaction.address,
                transaction.value,
                transaction.data,
                transaction.nonce!!,
                info.status.transaction.operation.ordinal
            )
        }
            .subscribeOn(Schedulers.computation())
            .flatMapCompletable {
                signaturePushRepository.request(info.selectedSafe, it)
            }
            .mapToResult()

    override fun addSignature(encodedSignatureUrl: String): Completable =
        signatureStore.loadSingingInfo()
            .flatMap { (safe, transaction) ->
                (GnoSafeUrlParser.parse(encodedSignatureUrl) as? GnoSafeUrlParser.Parsed.SignResponse)?.let {
                    checkSignature(safe, transaction, it.signature)
                }
                        ?: throw SimpleLocalizedException(context.getString(R.string.invalid_signature_uri))
            }
            .map(signatureStore::add)
            .toCompletable()

    override fun loadExecuteInfo(safeAddress: Solidity.Address, transaction: SafeTransaction): Observable<Result<Info>> =
        transactionRepository.loadExecuteInformation(safeAddress, transaction)
            .flatMapObservable { info ->
                // Observe local signature store
                signatureStore.flatMapInfo(safeAddress, info)
                    .onErrorReturnItem(emptyMap())
                    .map<Result<Info>> { DataResult(Info(safeAddress, info, it)) }
            }
            .onErrorReturn { ErrorResult(it) }


    override fun submitTransaction(safeAddress: Solidity.Address, transaction: SafeTransaction): Single<Result<Solidity.Address>> =
        transactionRepository.loadExecuteInformation(safeAddress, transaction)
            .flatMapCompletable { info ->
                // Observe local signature store
                signatureStore.load()
                    .map { info.check(it); it }
                    .flatMapCompletable { transactionRepository.submit(safeAddress, info.transaction, it, info.isOwner) }
            }
            .andThen(Single.just(safeAddress))
            .onErrorResumeNext({
                if (it is HttpException && it.code() == HttpCodes.UNAUTHORIZED)
                    Single.error(IllegalStateException())
                else
                    errorHandler.single(it)
            })
            .mapToResult()

    override fun estimateTransaction(info: Info): Single<Result<FeeEstimate>> =
        transactionRepository.estimateFees(info.selectedSafe, info.status.transaction, info.signatures, info.status.isOwner).mapToResult()

    override fun loadExecutableTransaction(safeAddress: Solidity.Address, transaction: SafeTransaction): Single<Transaction> =
        transactionRepository.loadExecuteInformation(safeAddress, transaction)
            .flatMap { info ->
                // Observe local signature store
                signatureStore.load()
                    .map { info.check(it); it }
                    .flatMap { transactionRepository.loadExecutableTransaction(safeAddress, info.transaction, it, info.isOwner) }
            }

    override fun signTransaction(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        sendViaPush: Boolean
    ): Single<Result<Pair<String, Bitmap?>>> =
        transactionRepository.sign(safeAddress, transaction)
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

    private fun TransactionRepository.ExecuteInformation.check(signatures: Map<Solidity.Address, Signature>): TransactionRepository.ExecuteInformation =
        when {
            requiredConfirmation - signatures.size - (if (isOwner) 1 else 0) > 0 ->
                throw SimpleLocalizedException(context.getString(R.string.error_not_enough_confirmations))
            else -> this
        }
}
