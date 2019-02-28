package pm.gnosis.heimdall.ui.messagesigning

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import pm.gnosis.eip712.*
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.utils.nullOnThrow
import javax.inject.Inject

class ReviewPayloadViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val eiP712JsonParser: EIP712JsonParser,
    private val gnosisSafeRepository: GnosisSafeRepository
) : ReviewPayloadContract() {
    override val uiEvents = PublishSubject.create<UIEvent>()

    private data class ViewArguments(val payload: String, val safe: Solidity.Address)

    private lateinit var viewArguments: ViewArguments

    private var selectedSafe: Solidity.Address? = null

    override fun setup(payload: String, safe: Solidity.Address) {
        viewArguments = ViewArguments(payload = payload, safe = safe)
    }

    private val safesObservable by lazy { gnosisSafeRepository.observeSafes().toObservable() }

    override fun observe() = uiEvents.compose(transformer)
        .subscribeOn(Schedulers.computation())
        .replay(1)
        .autoConnect()

    private val transformer = ObservableTransformer<UIEvent, ViewUpdate> { events ->
        events.publish {
            Observable.merge(
                events.ofType(UIEvent.ConfirmPayloadClick::class.java).compose(confirmPayloadTransformer),
                events.ofType(UIEvent.SelectSafe::class.java).compose(selectSafeClickTransformer),
                safesObservable.compose(safesTransformer)
            )
        }
            .scan(ViewUpdate(payload = viewArguments.payload, isLoading = false))
            { previous: ViewUpdate, result: Result -> stateReducer(previous, result) }
    }

    private fun stateReducer(previous: ViewUpdate, result: Result): ViewUpdate =
        when (result) {
            is Result.PayloadConfirmed -> ViewUpdate(
                payload = viewArguments.payload,
                isLoading = false,
                targetScreen = TargetScreen.MessageSignaturesActivity(
                    safe = viewArguments.safe,
                    payload = viewArguments.payload,
                    appSignature = result.appSignature
                )
            )
            is Result.ConfirmationException -> ViewUpdate(
                payload = viewArguments.payload,
                isLoading = false,
                targetScreen = null
            )
            is Result.ConfirmationInProgress -> ViewUpdate(
                payload = viewArguments.payload,
                isLoading = true,
                targetScreen = null
            )
            is Result.Safes -> previous.copy(safes = result.safes)
            is Result.Ignored -> previous.copy()
        }

    private val selectSafeClickTransformer = ObservableTransformer<UIEvent.SelectSafe, Result> { selectSafeClicks ->
        selectSafeClicks
            .doOnNext { selectedSafe = it.safe }
            .map { Result.Ignored }
    }

    private val confirmPayloadTransformer = ObservableTransformer<UIEvent.ConfirmPayloadClick, Result> { events ->
        events.flatMap { confirmPayload() }
    }

    private val safesTransformer = ObservableTransformer<List<Safe>, Result> { safesEvents ->
        safesEvents
            .map { Result.Safes(it) }
    }

    private fun confirmPayload() =
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
            // Sign the hash
            .flatMapSingle { safeMessageHash ->
                accountsRepository.sign(selectedSafe!!, safeMessageHash)
                    .map<Result> { Result.PayloadConfirmed(it) }
                    .onErrorResumeNext { Single.error(ErrorSigningHash) }
            }
            .startWith(Result.ConfirmationInProgress)
            .onErrorReturn { Result.ConfirmationException(it) }

    private sealed class Result {
        data class PayloadConfirmed(val appSignature: Signature) : Result()
        data class ConfirmationException(val throwable: Throwable) : Result()
        object ConfirmationInProgress : Result()
        data class Safes(val safes: List<Safe>) : Result()
        object Ignored : Result()
    }
}
