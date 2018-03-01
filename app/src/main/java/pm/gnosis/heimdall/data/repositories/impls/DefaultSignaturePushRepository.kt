package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.data.remote.MessageQueueRepository
import pm.gnosis.heimdall.data.remote.PushServiceApi
import pm.gnosis.heimdall.data.remote.models.RequestSignatureData
import pm.gnosis.heimdall.data.remote.models.SendSignatureData
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.SignaturePushRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.utils.GnoSafeUrlParser
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.utils.*
import java.math.BigInteger
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSignaturePushRepository @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val messageQueueRepository: MessageQueueRepository,
    private val preferencesManager: PreferencesManager,
    private val pushServiceApi: PushServiceApi,
    private val safeRepository: GnosisSafeRepository,
    private val transactionRepository: GnosisSafeTransactionRepository
) : SignaturePushRepository {

    private var cachedSafes: List<Safe>? = null

    override fun init() {
        preferencesManager.prefs.getStringSet(PREFS_OBSERVED_SAFES, emptySet()).forEach {
            messageQueueRepository.unsubscribe(RESPOND_SIGNATURE_TOPIC_PREFIX + it)
        }
        preferencesManager.prefs.edit { putStringSet(PREFS_OBSERVED_SAFES, emptySet()) }
        safeRepository.observeDeployedSafes()
            .subscribe(::handleSafes)
    }

    private fun handleSafes(safes: List<Safe>) {
        cachedSafes?.forEach {
            messageQueueRepository.unsubscribe(REQUEST_SIGNATURE_TOPIC_PREFIX + cleanAddress(it.address))
        }
        cachedSafes = safes
        safes.forEach {
            messageQueueRepository.subscribe(REQUEST_SIGNATURE_TOPIC_PREFIX + cleanAddress(it.address))
        }
    }

    override fun request(safe: BigInteger, data: String): Completable {
        val safeAddress = cleanAddress(safe)
        return accountsRepository.sign(Sha3Utils.keccak(safeAddress.hexStringToByteArray()))
            .flatMapCompletable {
                pushServiceApi.requestSignatures(it.toString(), safeAddress, RequestSignatureData(data))
            }
    }

    override fun send(safe: BigInteger, transaction: Transaction, signature: Signature): Completable {
        return transactionRepository.calculateHash(safe, transaction).flatMapCompletable {
            pushServiceApi.sendSignature(cleanAddress(safe), SendSignatureData(GnoSafeUrlParser.signResponse(signature), it.toHexString()))
        }
    }

    private val observedSafes = HashMap<BigInteger, ReceiveSignatureObservable>()

    override fun receivedSignature(topic: String?, signature: Signature) {
        if (topic?.startsWith(FCM_TOPICS_PREFIX + RESPOND_SIGNATURE_TOPIC_PREFIX) != true) {
            return
        }
        topic.removePrefix(FCM_TOPICS_PREFIX + RESPOND_SIGNATURE_TOPIC_PREFIX).hexAsBigIntegerOrNull()?.let {
            observedSafes[it]
        }?.publish(signature)
    }

    override fun observe(safe: BigInteger): Observable<Signature> =
        observedSafes.getOrPut(safe, {
            ReceiveSignatureObservable(messageQueueRepository, preferencesManager, {
                observedSafes.remove(safe)
            }, cleanAddress(safe))
        }).observe()

    private fun cleanAddress(address: BigInteger) =
        address.asEthereumAddressString().toLowerCase().removeHexPrefix()

    private class ReceiveSignatureObservable(
        private val messageQueueRepository: MessageQueueRepository,
        private val preferencesManager: PreferencesManager,
        private val releaseCallback: (ReceiveSignatureObservable) -> Unit,
        private val safeAddress: String
    ) : ObservableOnSubscribe<Signature> {
        private val emitters = CopyOnWriteArraySet<ObservableEmitter<Signature>>()

        private fun attach() {
            updateObservedSafes({ it.add(safeAddress) })
            messageQueueRepository.subscribe(RESPOND_SIGNATURE_TOPIC_PREFIX + safeAddress)
        }

        private fun release() {
            messageQueueRepository.unsubscribe(RESPOND_SIGNATURE_TOPIC_PREFIX + safeAddress)
            updateObservedSafes({ it.remove(safeAddress) })
        }

        override fun subscribe(e: ObservableEmitter<Signature>) {
            if (emitters.isEmpty()) {
                attach()
            }
            emitters.add(e)
            e.setCancellable {
                emitters.remove(e)
                if (emitters.isEmpty()) {
                    releaseCallback.invoke(this)
                    release()
                }
            }
        }

        fun observe(): Observable<Signature> = Observable.create(this)

        fun publish(signature: Signature) {
            emitters.forEach { it.onNext(signature) }
        }

        private fun updateObservedSafes(update: (MutableSet<String>) -> Unit) {
            val observedSafes = HashSet(preferencesManager.prefs.getStringSet(PREFS_OBSERVED_SAFES, emptySet()))
            update(observedSafes)
            preferencesManager.prefs.edit { putStringSet(PREFS_OBSERVED_SAFES, observedSafes) }
        }

    }

    companion object {
        private const val PREFS_OBSERVED_SAFES = "default_signature_push_repo.string_set.observed_safes"
        private const val REQUEST_SIGNATURE_TOPIC_PREFIX = "request_signature."
        private const val RESPOND_SIGNATURE_TOPIC_PREFIX = "respond_signature."
        private const val FCM_TOPICS_PREFIX = "/topics/"
    }
}
