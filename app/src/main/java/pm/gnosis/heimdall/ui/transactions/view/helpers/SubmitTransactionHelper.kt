package pm.gnosis.heimdall.ui.transactions.view.helpers

import com.gojuno.koptional.Optional
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.helpers.SignatureStore
import pm.gnosis.heimdall.ui.transactions.view.TransactionInfoViewHolder
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper.Events
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper.ViewUpdate
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.heimdall.utils.emitAndNext
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.nullOnThrow
import pm.gnosis.utils.toHexString
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

interface SubmitTransactionHelper {

    fun setup(
        safe: Solidity.Address,
        executionInfo: (SafeTransaction) -> Single<TransactionExecutionRepository.ExecuteInformation>,
        referenceId: Long? = null
    )

    fun observe(
        events: Events,
        transactionData: TransactionData,
        initialSignatures: Set<Signature>? = null
    ): Observable<Result<ViewUpdate>>

    data class Events(val retry: Observable<Unit>, val requestConfirmations: Observable<Unit>, val submit: Observable<Unit>)

    sealed class ViewUpdate {
        data class TransactionInfo(val viewHolder: TransactionInfoViewHolder) : ViewUpdate()
        data class Estimate(
            val gasToken: ERC20TokenWithBalance,
            val networkFee: BigInteger,
            val assetBalanceAfterTransfer: ERC20TokenWithBalance?, // null if gasToken and assetToken are the same
            val sufficientFunds: Boolean
        ) : ViewUpdate()

        object EstimateError : ViewUpdate()
        data class RequireConfirmations(val authenticatorInfo: AuthenticatorInfo, val hash: String) : ViewUpdate()
        data class Confirmations(val isReady: Boolean) : ViewUpdate()
        object ConfirmationsRequested : ViewUpdate()
        object ConfirmationsError : ViewUpdate()
        object TransactionRejected : ViewUpdate()
        data class TransactionSubmitted(val txHash: String?) : ViewUpdate()
    }
}

class DefaultSubmitTransactionHelper @Inject constructor(
    private val executionRepository: TransactionExecutionRepository,
    private val safeRepository: GnosisSafeRepository,
    private val signaturePushRepository: PushServiceRepository,
    private val signatureStore: SignatureStore,
    private val tokenRepository: TokenRepository,
    private val transactionViewHolderBuilder: TransactionViewHolderBuilder
) : SubmitTransactionHelper {

    private lateinit var safe: Solidity.Address
    private lateinit var executionInfo: (SafeTransaction) -> Single<TransactionExecutionRepository.ExecuteInformation>
    private var referenceId: Long? = null

    override fun setup(
        safe: Solidity.Address,
        executionInfo: (SafeTransaction) -> Single<TransactionExecutionRepository.ExecuteInformation>,
        referenceId: Long?
    ) {
        this.safe = safe
        this.referenceId = referenceId
        this.executionInfo = executionInfo
    }

    override fun observe(
        events: Events,
        transactionData: TransactionData,
        initialSignatures: Set<Signature>?
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
        initialSignatures: Set<Signature>?
    ) =
        events.retry
            .subscribeOn(AndroidSchedulers.mainThread())
            .startWith(Unit)
            .switchMapSingle { viewHolder.loadTransaction() }
            .switchMapSingle { tx -> executionInfo(tx).mapToResult() }
            .switchMapSingle {
                it.mapSingle({ execInfo ->
                    Single.zip(
                        tokenRepository.loadToken(execInfo.gasToken),
                        viewHolder.loadAssetChange(), BiFunction { gasToken: ERC20Token, assetChange: Optional<ERC20TokenWithBalance> ->
                            gasToken to assetChange.toNullable()
                        })
                        .flatMap { (gasToken, assetChange) ->
                            val networkFee = execInfo.gasCosts()
                            if (assetChange?.token == null || assetChange.token == gasToken) {
                                val balanceAfterTransfer = execInfo.balance - networkFee - (assetChange?.balance ?: BigInteger.ZERO)
                                val canSubmit = balanceAfterTransfer >= BigInteger.ZERO
                                Single.just(ViewUpdate.Estimate(ERC20TokenWithBalance(gasToken, balanceAfterTransfer), networkFee, null, canSubmit))
                            } else {
                                tokenRepository.loadTokenBalances(safe, listOf(assetChange.token))
                                    .firstOrError()
                                    .map { balances ->
                                        val (_, assetBalance) = balances.first()
                                        val balanceAfterTransfer = execInfo.balance - networkFee
                                        val assetBalanceAfterTransfer = (assetBalance ?: BigInteger.ZERO) - (assetChange.balance ?: BigInteger.ZERO)
                                        val canSubmit = balanceAfterTransfer >= BigInteger.ZERO && assetBalanceAfterTransfer >= BigInteger.ZERO
                                        ViewUpdate.Estimate(
                                            ERC20TokenWithBalance(gasToken, balanceAfterTransfer), networkFee,
                                            ERC20TokenWithBalance(assetChange.token, assetBalanceAfterTransfer), canSubmit
                                        )
                                    }
                                    .onErrorReturn {
                                        val balanceAfterTransfer = execInfo.balance - networkFee
                                        val gasBalanceAfterTransfer = ERC20TokenWithBalance(gasToken, balanceAfterTransfer)
                                        ViewUpdate.Estimate(gasBalanceAfterTransfer, networkFee, null, balanceAfterTransfer >= BigInteger.ZERO)
                                    }
                            }
                        }
                        .map { update -> execInfo to update }
                        .mapToResult()
                })
            }
            .emitAndNext(
                emit = {
                    it.map { (_, update) -> update }
                },
                next = { confirmations(events, it, initialSignatures) })

    private fun confirmations(
        events: Events,
        params: Result<Pair<TransactionExecutionRepository.ExecuteInformation, ViewUpdate.Estimate>>,
        initialSignatures: Set<Signature>?
    ) =
        (params as? DataResult)?.data?.let { (execInfo, update) ->
            if (update.sufficientFunds) {
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
        initialSignatures: Set<Signature>?
    ) =
        (initialSignatures?.let {
            Single.zip(
                it.map {
                    executionRepository.checkConfirmation(
                        safe, params.transaction, params.txGas, params.dataGas, params.gasPrice, params.gasToken, it, params.safeVersion
                    )
                }
            ) { results ->
                @Suppress("UNCHECKED_CAST")
                results.associate { entry -> (entry as Pair<Solidity.Address, Signature>) }
            }
                .onErrorReturn { emptyMap() }
        } ?: Single.just(emptyMap()))
            .flatMapObservable {
                signatureStore.flatMapInfo(
                    safe, params, it
                )
            }.publish {
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
            signatureStore.observe().map { confirmations ->
                val targets = params.owners - params.sender - confirmations.keys
                if (targets.isEmpty() || params.requiredConfirmation == confirmations.size + (if (params.isOwner) 1 else 0))
                    return@map false to null
                for (target in targets) {
                    nullOnThrow { safeRepository.loadAuthenticatorInfo(target) }?.let {
                        return@map true to it
                    }
                }
                true to AuthenticatorInfo(AuthenticatorInfo.Type.EXTENSION, targets.first(), null)
            }
                .filter { (needsConfirmations, _) -> needsConfirmations }
                .distinctUntilChanged { (_, info) -> info!! }
                .switchMap { (_, info) ->
                    var update = Observable.just<Result<ViewUpdate>>(DataResult(ViewUpdate.RequireConfirmations(info!!, params.transactionHash)))
                    if (info.type == AuthenticatorInfo.Type.EXTENSION) {
                        update = update.concatWith(requestConfirmationViaPush(events, params))
                    }
                    update
                }

    private fun requestConfirmationViaPush(
        events: Events,
        params: TransactionExecutionRepository.ExecuteInformation
    ) =
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
                    safe, params.transaction, params.txGas, params.dataGas, params.gasPrice, params.gasToken, params.safeVersion
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
                            params.gasToken,
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
            safe, params.transaction, signatures, params.isOwner, params.txGas, params.dataGas, params.gasPrice, params.gasToken, params.safeVersion,
            referenceId = referenceId
        )
            .flatMap { txHash ->
                val targets = (params.owners - params.sender).toSet()
                if (targets.isEmpty()) {
                    // Nothing to push
                    return@flatMap Single.just(txHash)
                }
                return@flatMap signaturePushRepository.propagateSubmittedTransaction(params.transactionHash, txHash, safe, targets)
                    // Ignore error here ... if push fails ... it fails
                    .doOnError(Timber::e)
                    .onErrorComplete()
                    .andThen(Single.just(txHash))
            }
            .flatMapObservable {
                Observable.just<ViewUpdate>(
                    ViewUpdate.TransactionSubmitted(it)
                )
            }
            .onErrorResumeNext { t: Throwable ->
                // Propagate error to display snackbar then propagate status
                Observable.just<ViewUpdate>(
                    ViewUpdate.TransactionSubmitted(null)
                ).concatWith(Observable.error(t))
            }
            .mapToResult()

    private fun observeIncomingConfirmations(params: TransactionExecutionRepository.ExecuteInformation) =
        signaturePushRepository.observe(params.transactionHash)
            .flatMap {
                when (it) {
                    is PushServiceRepository.TransactionResponse.Confirmed ->
                        executionRepository.checkConfirmation(
                            safe, params.transaction, params.txGas, params.dataGas, params.gasPrice, params.gasToken, it.signature, params.safeVersion
                        )
                            .map(signatureStore::add)
                            .flatMapObservable { Observable.empty<Result<ViewUpdate>>() }
                            .onErrorResumeNext { e: Throwable -> Observable.just(ErrorResult(e)) }
                    is PushServiceRepository.TransactionResponse.Rejected ->
                        executionRepository.checkRejection(
                            safe, params.transaction, params.txGas, params.dataGas, params.gasPrice, params.gasToken, it.signature, params.safeVersion
                        )
                            .filter { (sender) -> params.owners.contains(sender) }
                            .map<Result<ViewUpdate>> { DataResult(ViewUpdate.TransactionRejected) }
                            .toObservable()
                            .onErrorResumeNext { e: Throwable -> Observable.just(ErrorResult(e)) }
                }
            }

}
