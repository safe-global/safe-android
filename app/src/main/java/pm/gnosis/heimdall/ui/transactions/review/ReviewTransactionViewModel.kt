package pm.gnosis.heimdall.ui.transactions.review

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import pm.gnosis.heimdall.data.repositories.SignaturePushRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.helpers.SignatureStore
import pm.gnosis.heimdall.ui.transactions.review.viewholders.AssetTransferViewHolder
import pm.gnosis.heimdall.ui.transactions.review.viewholders.GenericTransactionViewHolder
import pm.gnosis.heimdall.utils.emitAndNext
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.asEthereumAddressString
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ReviewTransactionViewModel @Inject constructor(
    private val addressHelper: AddressHelper,
    private val executionRepository: TransactionExecutionRepository,
    private val signaturePushRepository: SignaturePushRepository,
    private val signatureStore: SignatureStore,
    private val tokenRepository: TokenRepository
) : ReviewTransactionContract() {

    private var safe: Solidity.Address? = null

    private val cachedState = mutableMapOf<SafeTransaction, TransactionExecutionRepository.ExecuteInformation>()

    override fun setup(safe: Solidity.Address) {
        if (this.safe != safe) {
            cachedState.clear()
        }
        this.safe = safe
    }

    private fun txParams(transaction: SafeTransaction): Single<TransactionExecutionRepository.ExecuteInformation> {
        val safe = safe ?: return Single.error(IllegalStateException())
        return cachedState[transaction]?.let { Single.just(it) }
                ?: executionRepository.loadExecuteInformation(safe, transaction)
                    .doOnSuccess { cachedState[transaction] = it }
    }

    override fun observe(events: Events, transactionData: TransactionData): Observable<Result<ViewUpdate>> =
        transactionViewHolder(transactionData)
            .emitAndNext(
                emit = {
                    DataResult(
                        ViewUpdate.TransactionInfo(
                            it
                        )
                    )
                },
                next = { estimation(events, it) })

    private fun estimation(events: Events, viewHolder: TransactionInfoViewHolder) =
        viewHolder.loadTransaction().flatMapObservable { tx ->
            events.retry
                .subscribeOn(AndroidSchedulers.mainThread())
                .startWith(Unit)
                .switchMapSingle { txParams(tx).mapToResult() }
        }.emitAndNext(
            emit = { it.map {
                ViewUpdate.Estimate(
                    Wei(
                        it.estimate.multiply(
                            it.gasPrice
                        )
                    ), it.balance
                )
            } },
            next = { confirmations(events, it) })

    private fun confirmations(events: Events, params: Result<TransactionExecutionRepository.ExecuteInformation>) =
        (params as? DataResult)?.data?.let {
            // Once we have the execution information we can setup everything related to requesting and receiving confirmation
            Observable.merge(
                requestConfirmation(events, it),
                observeConfirmationStore(events, it),
                observeIncomingConfirmations(it)
            )
        } ?: Observable.just<Result<ViewUpdate>>(
            DataResult(ViewUpdate.EstimateError)
        )

    private fun requestConfirmation(events: Events, params: TransactionExecutionRepository.ExecuteInformation) =
        events.requestConfirmations
            .startWith(Unit)
            .subscribeOn(AndroidSchedulers.mainThread())
            .emitAndNext(
                emit = { ViewUpdate.ConfirmationsRequested },
                next = {
                    // TODO: replace with extension signature logic
                    Observable.timer(3, TimeUnit.SECONDS)
                        .flatMapSingle {
                            executionRepository.sign(safe!!, params.transaction)
                        }
                        .map {
                            signaturePushRepository.receivedSignature(
                                "/topics/respond_signature.${safe!!.asEthereumAddressString()}",
                                it
                            )
                        }
                        .flatMap { Observable.empty<ViewUpdate>() }
                }
            )
            .mapToResult()

    private fun observeConfirmationStore(events: Events, params: TransactionExecutionRepository.ExecuteInformation) =
        signatureStore.flatMapInfo(
            safe!!, params
        ).publish {
            Observable.merge(
                it.map {
                    DataResult(
                        ViewUpdate.Confirmations(
                            it.size >= params.requiredConfirmation
                        )
                    )
                },
                it.switchMap { signatures ->
                    events.submit
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .switchMap { submitTransaction(params, signatures) }
                }
            )
        }

    private fun submitTransaction(params: TransactionExecutionRepository.ExecuteInformation, signatures: Map<Solidity.Address, Signature>) =
        executionRepository.submit(safe!!, params.transaction, signatures, false)
            .andThen(
                Observable.just<ViewUpdate>(
                    ViewUpdate.TransactionSubmitted(
                        true
                    )
                )
            )
            .onErrorResumeNext { t: Throwable ->
                // Propagate error to display snackbar then propagate status
                Observable.just<ViewUpdate>(
                    ViewUpdate.TransactionSubmitted(
                        false
                    )
                ).concatWith(Observable.error(t))
            }
            .mapToResult()

    private fun observeIncomingConfirmations(params: TransactionExecutionRepository.ExecuteInformation) =
        signaturePushRepository.observe(safe!!)
            .flatMapSingle {
                executionRepository.checkSignature(safe!!, params.transaction, it)
            }
            .map(signatureStore::add)
            .flatMap { Observable.empty<Result<ViewUpdate>>() }
            .onErrorResumeNext { e: Throwable -> Observable.just(ErrorResult(e)) }

    private fun transactionViewHolder(transactionData: TransactionData): Single<TransactionInfoViewHolder> =
        Single.fromCallable {
            val safe = safe ?: throw IllegalStateException()
            when (transactionData) {
                is TransactionData.Generic ->
                    GenericTransactionViewHolder(safe, transactionData, addressHelper)
                is TransactionData.RecoverSafe -> TODO()
                is TransactionData.ReplaceRecoveryPhrase -> TODO()
                is TransactionData.AssetTransfer ->
                    AssetTransferViewHolder(
                        safe,
                        transactionData,
                        addressHelper,
                        tokenRepository
                    )
            }
        }

}
