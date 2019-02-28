package pm.gnosis.heimdall.ui.messagesigning

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import pm.gnosis.eip712.*
import pm.gnosis.heimdall.data.remote.models.push.PushMessage
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.helpers.CryptoHelper
import pm.gnosis.heimdall.helpers.SignatureStore
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.utils.toHexString
import java.math.BigInteger
import javax.inject.Inject

class CollectMessageSignaturesViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val eiP712JsonParser: EIP712JsonParser,
    private val pushServiceRepository: PushServiceRepository,
    private val messageSignatureStore: SignatureStore,
    private val cryptoHelper: CryptoHelper
) : CollectMessageSignaturesContract() {
    override val uiEvents = PublishSubject.create<UIEvent>()

    data class ViewArguments(
        val payload: String,
        val safe: Solidity.Address,
        val threshold: Long,
        val owners: List<Solidity.Address>,
        val deviceSignature: Signature
    )

    private lateinit var viewArguments: ViewArguments

    override fun setup(
        payload: String,
        safe: Solidity.Address,
        threshold: Long,
        owners: List<Solidity.Address>,
        deviceSignature: Signature
    ) {
        viewArguments = ViewArguments(payload = payload, safe = safe, threshold = threshold, owners = owners, deviceSignature = deviceSignature)
    }

    override fun observe(): Observable<ViewUpdate> =
        uiEvents
            .compose(transformer)
            .replay(1)
            .autoConnect()

    private val storeObservable by lazy { messageSignatureStore.observe().map { setOf(*it.values.toTypedArray()) } }

    private val payloadHash by lazy {
        val hash = eiP712JsonParser.parseMessage(viewArguments.payload).let { typedDataHash(message = it.message, domain = it.domain) }
        val safeMessageStruct = Struct712(
            typeName = "SafeMessage",
            parameters = listOf(
                Struct712Parameter(
                    name = "message",
                    type = Literal712(typeName = "bytes", value = Solidity.Bytes(hash))
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

    private val transformer = ObservableTransformer<UIEvent, ViewUpdate> { events ->
        events
            .publish {
                Observable.merge(
                    listOf(
                        events.ofType(UIEvent.RequestSignaturesClick::class.java).compose(requestSignaturesClickTransformer),
                        events.ofType(UIEvent.ViewLoaded::class.java).compose(viewLoadedTransformer),
                        storeObservable.compose(finalSignatureTransformer),
                        storeObservable.compose(countOwnerSignaturesTransformer),
                        pushServiceRepository.observeTypedDataConfirmationPushes().compose(incomingSignedMessagesTransformer)
                    )
                )
            }
            .scan(
                ViewUpdate(
                    payload = viewArguments.payload,
                    signature = null,
                    threshold = viewArguments.threshold,
                    signaturesReceived = 0,
                    inProgress = false
                )
            ) { previous, update -> stateReducer(previous, update) }
    }

    private fun stateReducer(previous: ViewUpdate, update: Result): ViewUpdate =
        when (update) {
            is Result.RequestSent -> previous.copy(inProgress = false)
            is Result.RequestFailed -> previous.copy(inProgress = false)
            is Result.FinalSignature -> previous.copy(signature = update.signature)
            is Result.Count -> previous.copy(signaturesReceived = update.ownerSignaturesCount)
            is Result.Ignored -> previous.copy()
            is Result.InProgress -> previous.copy(inProgress = true)
        }

    private val viewLoadedTransformer = ObservableTransformer<UIEvent.ViewLoaded, Result> { viewLoadedEvents ->
        viewLoadedEvents.flatMap { viewLoadedEvent ->
            Observable.just(viewLoadedEvent.deviceSignature)
                .compose(storeSignatureTransformer)
                .map<Result> { Result.Ignored }
                .onErrorReturn { Result.Ignored }
        }
    }

    private val requestSignaturesClickTransformer = ObservableTransformer<UIEvent.RequestSignaturesClick, Result> { clickEvents ->
        clickEvents
            .flatMap {
                requestSignatures(owners = viewArguments.owners)
                    .andThen(Observable.just<Result>(Result.RequestSent))
                    .onErrorReturn { Result.RequestFailed(it) }
                    .startWith(Result.InProgress)
            }
    }

    private fun requestSignatures(owners: List<Solidity.Address>) =
        accountsRepository.signingOwner(viewArguments.safe)
            .map { currentAccount -> owners.minus(currentAccount.address) }
            // Send request
            .flatMapCompletable { targets ->
                pushServiceRepository.requestTypedDataConfirmations(
                    payload = viewArguments.payload,
                    safe = viewArguments.safe,
                    appSignature = viewArguments.deviceSignature,
                    targets = targets.toSet()
                )
            }

    private val finalSignatureTransformer = ObservableTransformer<Set<Signature>, Result> { storeEvents ->
        storeEvents
            .flatMapSingle { signatures ->
                Single.fromCallable { buildFinalSignature(signatures) }
                    .map<Result> { finalSignature -> Result.FinalSignature(BigInteger(finalSignature)) }
                    .onErrorReturn { Result.Ignored }
            }
    }

    private fun buildFinalSignature(ownerSignatures: Set<Signature>): ByteArray {
        return if (ownerSignatures.count() < viewArguments.threshold) throw IllegalArgumentException("Threshold not reached yet")
        else ownerSignatures.map { signature -> signature.toString().toByteArray() }.sortedBy { BigInteger(it) }.reduce { acc, bytes -> acc + bytes }
    }

    private val countOwnerSignaturesTransformer = ObservableTransformer<Set<Signature>, Result> { storeEvents ->
        storeEvents
            .flatMapSingle { signatures -> Single.just(Result.Count(ownerSignaturesCount = signatures.size.toLong())) }
    }

    private val incomingSignedMessagesTransformer = ObservableTransformer<PushMessage.SignTypedDataConfirmation, Result> { confirmationEvents ->
        confirmationEvents
            .flatMap {
                Observable
                    .fromCallable { Signature.from(it.signature.toHexString()) }
                    .compose(storeSignatureTransformer)
                    .map<Result> { Result.Ignored }
                    .onErrorReturn { Result.Ignored }
            }
    }

    private val storeSignatureTransformer = ObservableTransformer<Signature, Unit> { signedMessageEvents ->
        signedMessageEvents
            .flatMapSingle { signature ->
                Single.just(
                    cryptoHelper.recover(payloadHash, signature)
                )
                    .filter { recoveredAddress -> viewArguments.owners.contains(recoveredAddress) }
                    .map { messageSignatureStore.add(Pair(it, signature)) }
                    .toSingle()
            }
    }

    private sealed class Result {
        object RequestSent : Result()
        data class RequestFailed(val throwable: Throwable) : Result()
        data class FinalSignature(val signature: BigInteger) : Result()
        data class Count(val ownerSignaturesCount: Long) : Result()
        object Ignored : Result()
        object InProgress : Result()
    }
}
