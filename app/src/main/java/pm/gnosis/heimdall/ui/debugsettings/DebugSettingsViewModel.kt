package pm.gnosis.heimdall.ui.debugsettings

import com.squareup.moshi.Moshi
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.data.remote.PushServiceRepository
import pm.gnosis.heimdall.data.remote.models.push.PushServiceTemporaryAuthorization
import pm.gnosis.model.Solidity
import java.math.BigInteger
import javax.inject.Inject

class DebugSettingsViewModel @Inject constructor(
    private val moshi: Moshi,
    private val pushServiceRepository: PushServiceRepository
) : DebugSettingsContract() {
    override fun forceSyncAuthentication() = pushServiceRepository.syncAuthentication(true)

    override fun pair(payload: String): Completable =
        parseChromeExtensionPayload(payload)
            .flatMapCompletable { pushServiceRepository.pair(it).toCompletable() }

    private fun parseChromeExtensionPayload(payload: String): Single<PushServiceTemporaryAuthorization> =
        Single.fromCallable { moshi.adapter(PushServiceTemporaryAuthorization::class.java).fromJson(payload) }
            .subscribeOn(Schedulers.io())

    override fun sendTestSafeCreationPush(): Completable =
        pushServiceRepository.sendSafeCreationNotification(Solidity.Address(BigInteger.ZERO), setOf(Solidity.Address(BigInteger.ONE)))
}
