package pm.gnosis.heimdall.ui.messagesigning

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import pm.gnosis.eip712.*
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.utils.hexStringToByteArray
import pm.gnosis.utils.nullOnThrow
import javax.inject.Inject

class ConfirmMessageViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val eiP712JsonParser: EIP712JsonParser,
    private val pushServiceRepository: PushServiceRepository
) : ConfirmMessageContract() {

    override val uiEvents: PublishSubject<UIEvent> = PublishSubject.create<UIEvent>()

    data class ViewArguments(val payload: String, val safe: Solidity.Address, val signature: Signature)

    private lateinit var viewArguments: ViewArguments

    override fun observe() = uiEvents.compose(transformer)
        .subscribeOn(Schedulers.computation())
        .replay(1)
        .autoConnect()

    override fun setup(payload: String, safe: Solidity.Address, signature: Signature) {
        viewArguments = ViewArguments(payload = payload, safe = safe, signature = signature)
    }

    private val transformer = ObservableTransformer<UIEvent, ViewUpdate> { events ->
        events.publish { shared -> shared.ofType(UIEvent.ConfirmPayloadClick::class.java).compose(confirmPayloadTransformer) }
            .scan(ViewUpdate(payload = viewArguments.payload, isLoading = false))
            { previous: ViewUpdate, result: Result -> stateReducer(previous, result) }
    }

    private fun stateReducer(previous: ViewUpdate, result: Result): ViewUpdate =
        when (result) {
            is Result.ConfirmationSent -> previous.copy(
                isLoading = false,
                error = null,
                finishProcess = true
            )
            is Result.ConfirmationException -> previous.copy(
                isLoading = false,
                error = result.throwable,
                finishProcess = false
            )
            is Result.RejectPayload -> previous.copy(
                isLoading = false,
                error = null,
                finishProcess = true
            )
            is Result.ConfirmationInProgress -> previous.copy(
                isLoading = true,
                error = null,
                finishProcess = false
            )
        }

    private val confirmPayloadTransformer = ObservableTransformer<UIEvent.ConfirmPayloadClick, Result> { events ->
        events.flatMap { confirmPayload() }
    }

    private fun confirmPayload(): Observable<Result> =
        Observable
            // Get the hash of the payload (EIP712)
            .fromCallable {
                nullOnThrow { eiP712JsonParser.parseMessage(viewArguments.payload) }
                    ?.let { typedDataHash(message = it.message, domain = it.domain) }
                    ?: throw InvalidPayload
            }
            // Generate EIP712 hash for the payload
            // TODO: this can probably be extracted somewhere else...
            .map { payloadHash ->
                val safeMessageStruct = Struct712(
                    typeName = "SafeMessage",
                    parameters = listOf(
                        Struct712Parameter(
                            name = "message",
                            type = Literal712(typeName = "bytes", value = Solidity.Bytes(payloadHash))
                        )
                    )
                )

                val safeDomain = Struct712(
                    typeName = "EIP712Domain",
                    parameters = listOf(
                        Struct712Parameter(
                            name = "verifyingContract",
                            type = Literal712("address", viewArguments.safe)
                        )
                    )
                )
                typedDataHash(message = safeMessageStruct, domain = safeDomain)
            }
            // Recover the account that sent the request
            .flatMapSingle { safeMessageHash ->
                accountsRepository.recover(safeMessageHash, viewArguments.signature)
                    .map { it to safeMessageHash }
                    .onErrorResumeNext { Single.error(ErrorRecoveringSender) }
            }
            // Sign the hash
            .flatMapSingle { (requester, safeMessageHash) ->
                accountsRepository.sign(safeMessageHash)
                    .map { it.toString().hexStringToByteArray() }
                    .map { Triple(requester, it, safeMessageHash) }
                    .onErrorResumeNext { Single.error(ErrorSigningHash) }
            }
            // Send push to requester with result
            .flatMapCompletable { (requester, signatureBytes, safeMessageHash) ->
                pushServiceRepository.sendTypedDataConfirmation(hash = safeMessageHash, signature = signatureBytes, targets = setOf(requester))
                    .onErrorResumeNext { Completable.error(ErrorSendingPush) }
            }
            .andThen(Observable.just<Result>(Result.ConfirmationSent))
            .startWith(Result.ConfirmationInProgress)
            .onErrorReturn { Result.ConfirmationException(it) }


    private sealed class Result {
        object ConfirmationSent : Result()
        data class ConfirmationException(val throwable: Throwable) : Result()
        object ConfirmationInProgress : Result()
        object RejectPayload : Result()
    }
}
