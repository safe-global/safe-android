package pm.gnosis.heimdall.data.repositories.impls

import com.google.firebase.messaging.FirebaseMessaging
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.accounts.base.models.Signature
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.PreferencesManager
import pm.gnosis.heimdall.common.utils.edit
import pm.gnosis.heimdall.data.remote.PushServiceApi
import pm.gnosis.heimdall.data.remote.models.RequestSignatureData
import pm.gnosis.heimdall.data.remote.models.SendSignatureData
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.SignaturePushRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.utils.GnoSafeUrlParser
import pm.gnosis.models.Transaction
import pm.gnosis.utils.*
import java.math.BigInteger
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSignaturePushRepository @Inject constructor(
        private val accountsRepository: AccountsRepository,
        private val preferencesManager: PreferencesManager,
        private val pushServiceApi: PushServiceApi,
        private val safeRepository: GnosisSafeRepository,
        private val transactionRepository: GnosisSafeTransactionRepository
) : SignaturePushRepository {

    // TODO inject
    private val firebase = FirebaseMessaging.getInstance()

    private var cachedSafes: List<Safe>? = null

    override fun init() {
        preferencesManager.prefs.getStringSet(PREFS_OBSERVED_SAFES, emptySet()).forEach {
            firebase.subscribeToTopic(RESPOND_SIGNATURE_TOPIC_PREFIX + it)
        }
        safeRepository.observeDeployedSafes()
                .subscribe(::handleSafes)
    }

    private fun handleSafes(safes: List<Safe>) {
        cachedSafes?.forEach {
            firebase.unsubscribeFromTopic(REQUEST_SIGNATURE_TOPIC_PREFIX + it.address.asEthereumAddressString().toLowerCase().removeHexPrefix())
        }
        cachedSafes = safes
        safes.forEach {
            firebase.subscribeToTopic(REQUEST_SIGNATURE_TOPIC_PREFIX + it.address.asEthereumAddressString().toLowerCase().removeHexPrefix())
        }
    }

    override fun request(safe: BigInteger, data: String): Completable {
        val safeAddress = safe.asEthereumAddressString().removeHexPrefix()
        return accountsRepository.sign(Sha3Utils.keccak(safeAddress.hexStringToByteArray()))
                .flatMapCompletable {
                    pushServiceApi.requestSignatures(it.toString(), safeAddress, RequestSignatureData(data))
                }
    }

    override fun send(safe: BigInteger, transaction: Transaction, signature: Signature): Completable {
        val safeAddress = safe.asEthereumAddressString().removeHexPrefix()
        return transactionRepository.calculateHash(safe, transaction).flatMapCompletable {
            pushServiceApi.sendSignature(safeAddress, SendSignatureData(GnoSafeUrlParser.signResponse(signature), it.toHexString()))
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
                ReceiveSignatureObservable(firebase, preferencesManager, {
                    observedSafes.remove(safe)
                }, safe)
            }).observe()

    private class ReceiveSignatureObservable(
            private val firebase: FirebaseMessaging,
            private val preferencesManager: PreferencesManager,
            private val releaseCallback: (ReceiveSignatureObservable) -> Unit,
            safe: BigInteger
    ) : ObservableOnSubscribe<Signature> {
        private val safeAddress = safe.asEthereumAddressString().toLowerCase().removeHexPrefix()
        private val emitters = CopyOnWriteArraySet<ObservableEmitter<Signature>>()

        init {
            updateObservedSafes({ it.add(safeAddress) })
            firebase.subscribeToTopic(RESPOND_SIGNATURE_TOPIC_PREFIX + safeAddress)
        }

        private fun release() {
            firebase.unsubscribeFromTopic(RESPOND_SIGNATURE_TOPIC_PREFIX + safeAddress)
            updateObservedSafes({ it.remove(safeAddress) })
        }

        override fun subscribe(e: ObservableEmitter<Signature>) {
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
