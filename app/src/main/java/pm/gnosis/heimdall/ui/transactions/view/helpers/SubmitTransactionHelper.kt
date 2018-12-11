package pm.gnosis.heimdall.ui.transactions.view.helpers

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import pm.gnosis.heimdall.ERC20Contract
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.helpers.SignatureStore
import pm.gnosis.heimdall.ui.transactions.view.TransactionInfoViewHolder
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper.Events
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper.ViewUpdate
import pm.gnosis.heimdall.utils.emitAndNext
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.isSolidityMethod
import pm.gnosis.utils.removeSolidityMethodPrefix
import pm.gnosis.utils.toHexString
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

interface SubmitTransactionHelper {

    fun setup(
        safe: Solidity.Address,
        executionInfo: (SafeTransaction) -> Single<TransactionExecutionRepository.ExecuteInformation>
    )

    fun observe(
        events: Events,
        transactionData: TransactionData,
        initialSignatures: Map<Solidity.Address, Signature>? = null
    ): Observable<Result<ViewUpdate>>

    data class Events(val retry: Observable<Unit>, val requestConfirmations: Observable<Unit>, val submit: Observable<Unit>)

    sealed class ViewUpdate {
        data class TransactionInfo(val viewHolder: TransactionInfoViewHolder) : ViewUpdate()
        data class Estimate(val fees: BigInteger, val balance: BigInteger, val token: ERC20Token, val canSubmit: Boolean) : ViewUpdate()
        object EstimateError : ViewUpdate()
        data class Confirmations(val isReady: Boolean) : ViewUpdate()
        object ConfirmationsRequested : ViewUpdate()
        object ConfirmationsError : ViewUpdate()
        object TransactionRejected : ViewUpdate()
        data class TransactionSubmitted(val success: Boolean) : ViewUpdate()
    }
}

class DefaultSubmitTransactionHelper @Inject constructor(
    private val executionRepository: TransactionExecutionRepository,
    private val signaturePushRepository: PushServiceRepository,
    private val signatureStore: SignatureStore,
    private val tokenRepository: TokenRepository,
    private val transactionViewHolderBuilder: TransactionViewHolderBuilder
) : SubmitTransactionHelper {

    private lateinit var safe: Solidity.Address
    private lateinit var executionInfo: (SafeTransaction) -> Single<TransactionExecutionRepository.ExecuteInformation>

    override fun setup(
        safe: Solidity.Address,
        executionInfo: (SafeTransaction) -> Single<TransactionExecutionRepository.ExecuteInformation>
    ) {
        this.safe = safe
        this.executionInfo = executionInfo
    }

    override fun observe(
        events: Events,
        transactionData: TransactionData,
        initialSignatures: Map<Solidity.Address, Signature>?
    ): Observable<Result<ViewUpdate>> =
        transactionViewHolderBuilder.build(safe, transactionData)
            .emitAndNext(
                emit = {
                    DataResult(
                        ViewUpdate.TransactionInfo(
                            it
                        )
                    )
                },
                next = { estimation(events, it, initialSignatures) })

    private fun estimation(
        events: Events,
        viewHolder: TransactionInfoViewHolder,
        initialSignatures: Map<Solidity.Address, Signature>?
    ) =
        events.retry
            .subscribeOn(AndroidSchedulers.mainThread())
            .startWith(Unit)
            .switchMapSingle { viewHolder.loadTransaction() }
            .switchMapSingle { tx -> executionInfo(tx).mapToResult() }
            .switchMapSingle {
                it.mapSingle({ execInfo ->
                    tokenRepository.loadToken(execInfo.gasToken).map { token -> execInfo to token }.mapToResult()
                })
            }
            .map {
                it.map { (execInfo, token) ->
                    val gasCosts = execInfo.gasCosts()
                    // If we transfer our payment token we should add this to the required funds
                    val requiredFunds = gasCosts + when (execInfo.gasToken) {
                        ERC20Token.ETHER_TOKEN.address -> (execInfo.transaction.wrapped.value?.value ?: BigInteger.ZERO)
                        execInfo.transaction.wrapped.address -> {
                            if (execInfo.transaction.wrapped.data?.isSolidityMethod(ERC20Contract.Transfer.METHOD_ID) == true) {
                                val argData = execInfo.transaction.wrapped.data!!.removeSolidityMethodPrefix(ERC20Contract.Transfer.METHOD_ID)
                                ERC20Contract.Transfer.decodeArguments(argData)._value.value
                            } else {
                                BigInteger.ZERO
                            }
                        }
                        else -> BigInteger.ZERO
                    }
                    Triple(execInfo, token, execInfo.balance >= requiredFunds)
                }
            }
            .emitAndNext(
                emit = {
                    it.map { (execInfo, token, canSubmit) -> ViewUpdate.Estimate(execInfo.gasCosts(), execInfo.balance, token, canSubmit) }
                },
                next = { confirmations(events, it, initialSignatures) })

    private fun confirmations(
        events: Events,
        params: Result<Triple<TransactionExecutionRepository.ExecuteInformation, ERC20Token, Boolean>>,
        initialSignatures: Map<Solidity.Address, Signature>?
    ) =
        (params as? DataResult)?.data?.let { (execInfo, _, canSubmit) ->
            if (canSubmit) {
                // Once we have the execution information we can setup everything related to requesting and receiving confirmation
                Observable.merge(
                    observeConfirmationStore(events, execInfo, initialSignatures),
                    observeIncomingConfirmations(execInfo)
                )
            } else {
                // We cannot submit don't do anything
                Observable.empty()
            }
        } ?: Observable.just<Result<ViewUpdate>>(
            DataResult(ViewUpdate.EstimateError)
        )

    private fun observeConfirmationStore(
        events: Events,
        params: TransactionExecutionRepository.ExecuteInformation,
        initialSignatures: Map<Solidity.Address, Signature>?
    ) =
        signatureStore.flatMapInfo(
            safe, params, initialSignatures
        ).publish {
            Observable.merge(
                it.firstElement().flatMapObservable { requestConfirmation(events, params, it) },
                it.map {
                    val threshold = params.requiredConfirmation - (if (params.isOwner) 1 else 0)
                    DataResult(
                        ViewUpdate.Confirmations(
                            it.size >= threshold
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

    private fun requestConfirmation(
        events: Events,
        params: TransactionExecutionRepository.ExecuteInformation,
        initialConfirmations: Map<Solidity.Address, Signature>
    ) =
        if ((params.requiredConfirmation == initialConfirmations.size + (if (params.isOwner) 1 else 0)))
            Observable.empty<Result<ViewUpdate>>()
        else
            events.requestConfirmations
                .subscribeOn(AndroidSchedulers.mainThread())
                .startWith(Unit)
                .flatMapSingle { signatureStore.load() }
                .switchMapSingle {
                    val targets = params.owners - params.sender - it.keys
                    if (targets.isEmpty()) {
                        // Nothing to push
                        return@switchMapSingle Single.just(DataResult(Unit))
                    }
                    executionRepository.calculateHash(
                        safe, params.transaction, params.txGas, params.dataGas, params.gasPrice, params.gasToken
                    )
                        .flatMapCompletable { hash ->
                            signaturePushRepository.requestConfirmations(
                                hash.toHexString().addHexPrefix(),
                                safe,
                                params.transaction,
                                params.txGas,
                                params.dataGas,
                                params.operationalGas,
                                params.gasPrice,
                                targets.toSet()
                            )

                        }
                        .mapToResult()
                }
                .flatMap {
                    when (it) {
                        is DataResult -> Observable.just(DataResult(ViewUpdate.ConfirmationsRequested))
                        is ErrorResult -> Observable.fromArray(DataResult(ViewUpdate.ConfirmationsError), ErrorResult<ViewUpdate>(it.error))
                    }
                }

    private fun submitTransaction(params: TransactionExecutionRepository.ExecuteInformation, signatures: Map<Solidity.Address, Signature>) =
        executionRepository.submit(
            safe, params.transaction, signatures, params.isOwner, params.txGas, params.dataGas, params.gasPrice, params.gasToken
        )
            .flatMapCompletable {
                val targets = (params.owners - params.sender).toSet()
                if (targets.isEmpty()) {
                    // Nothing to push
                    return@flatMapCompletable Completable.complete()
                }
                signaturePushRepository.propagateSubmittedTransaction(params.transactionHash, it, targets)
                    // Ignore error here ... if push fails ... it fails
                    .doOnError(Timber::e)
                    .onErrorComplete()
            }
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
        signaturePushRepository.observe(params.transactionHash)
            .flatMap {
                when (it) {
                    is PushServiceRepository.TransactionResponse.Confirmed ->
                        executionRepository.checkConfirmation(
                            safe, params.transaction, params.txGas, params.dataGas, params.gasPrice, params.gasToken, it.signature
                        )
                            .map(signatureStore::add)
                            .flatMapObservable { _ -> Observable.empty<Result<ViewUpdate>>() }
                            .onErrorResumeNext { e: Throwable -> Observable.just(ErrorResult(e)) }
                    is PushServiceRepository.TransactionResponse.Rejected ->
                        executionRepository.checkRejection(
                            safe, params.transaction, params.txGas, params.dataGas, params.gasPrice, params.gasToken, it.signature
                        )
                            .filter { (sender) -> params.owners.contains(sender) }
                            .map<Result<ViewUpdate>> { DataResult(ViewUpdate.TransactionRejected) }
                            .toObservable()
                            .onErrorResumeNext { e: Throwable -> Observable.just(ErrorResult(e)) }
                }
            }

}
