package pm.gnosis.heimdall.ui.debugsettings

import com.squareup.moshi.Moshi
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.data.remote.models.push.PushServiceTemporaryAuthorization
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.asEthereumAddress
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
        Single.fromCallable { moshi.adapter(PushServiceTemporaryAuthorization::class.java).fromJson(payload)!! }
            .subscribeOn(Schedulers.io())

    override fun sendTestSafeCreationPush(chromeExtensionAddress: String): Single<Result<Unit>> =
        pushServiceRepository.propagateSafeCreation(Solidity.Address(BigInteger.ZERO), setOf(chromeExtensionAddress.asEthereumAddress()!!))
            .mapToResult()
}
