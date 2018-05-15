package pm.gnosis.heimdall.data.remote

import io.reactivex.Completable
import io.reactivex.Single
import pm.gnosis.heimdall.data.remote.models.push.PushServiceTemporaryAuthorization
import pm.gnosis.model.Solidity

interface PushServiceRepository {
    fun syncAuthentication(forced: Boolean = false)
    fun pair(temporaryAuthorization: PushServiceTemporaryAuthorization): Single<Solidity.Address>
    fun sendSafeCreationNotification(safeAddress: Solidity.Address, devicesToNotify: Set<Solidity.Address>): Completable
}
