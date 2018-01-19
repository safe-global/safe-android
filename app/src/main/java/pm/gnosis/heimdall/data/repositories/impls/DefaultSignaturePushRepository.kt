package pm.gnosis.heimdall.data.repositories.impls

import com.google.firebase.messaging.FirebaseMessaging
import io.reactivex.Completable
import io.reactivex.Observable
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.accounts.base.models.Signature
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.data.remote.PushServiceApi
import pm.gnosis.heimdall.data.remote.models.RequestSignatureData
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.SignaturePushRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexStringToByteArray
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.removeHexPrefix
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSignaturePushRepository @Inject constructor(
        private val accountsRepository: AccountsRepository,
        private val pushServiceApi: PushServiceApi,
        private val safeRepository: GnosisSafeRepository
) : SignaturePushRepository {

    // TODO inject
    private val firebase = FirebaseMessaging.getInstance()

    private var cachedSafes: List<Safe>? = null

    override fun init() {
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

    override fun observe(safe: BigInteger): Observable<Signature> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        private const val REQUEST_SIGNATURE_TOPIC_PREFIX = "request_signature."
    }
}
