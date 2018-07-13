package pm.gnosis.heimdall.ui.safe.pairing

import com.squareup.moshi.Moshi
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.data.remote.models.push.PushServiceTemporaryAuthorization
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.model.Solidity
import javax.inject.Inject

class PairingViewModel @Inject constructor(
    private val pushServiceRepository: PushServiceRepository,
    private val moshi: Moshi
) : PairingContract() {
    override fun pair(payload: String): Single<Solidity.Address> =
        parseChromeExtensionPayload(payload)
            .flatMap { pushServiceRepository.pair(it) }

    private fun parseChromeExtensionPayload(payload: String): Single<PushServiceTemporaryAuthorization> =
        Single.fromCallable { moshi.adapter(PushServiceTemporaryAuthorization::class.java).fromJson(payload)!! }
            .subscribeOn(Schedulers.io())
}
