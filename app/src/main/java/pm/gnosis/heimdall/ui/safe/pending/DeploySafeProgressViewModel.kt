package pm.gnosis.heimdall.ui.safe.pending

import io.reactivex.Flowable
import io.reactivex.Single
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.data.remote.RelayServiceApi
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.model.Solidity
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DeploySafeProgressViewModel @Inject constructor(
    private val gnosisSafeRepository: GnosisSafeRepository,
    private val relayServiceApi: RelayServiceApi
) : DeploySafeProgressContract() {

    private var safeAddress: Solidity.Address? = null

    override fun setup(safeAddress: Solidity.Address?) {
        this.safeAddress = safeAddress
    }

    override fun notifySafeFunded(): Single<Solidity.Address> {
        val address = safeAddress ?: return Single.error(IllegalStateException("Invalid transaction hash"))
        return gnosisSafeRepository.loadPendingSafe(address)
            .flatMap { pendingSafe ->
                relayServiceApi.notifySafeFunded(pendingSafe.address.asEthereumAddressChecksumString())
                    .andThen(Single.just(pendingSafe))
            }
            .flatMap { pendingSafe ->
                relayServiceApi.safeFundStatus(pendingSafe.address.asEthereumAddressChecksumString())
                    .map {
                        if (!it.safeDeployed) throw SafeNotDeployedException()
                        pendingSafe
                    }
                    // retry until safe is mined
                    .retryWhen { errors ->
                        errors.flatMap {
                            if (it is SafeNotDeployedException) Flowable.just(it).delay(DELAY_IN_SECONDS, TimeUnit.SECONDS)
                            else Flowable.error(it)
                        }
                    }
            }
            .flatMap { gnosisSafeRepository.pendingSafeToDeployedSafe(it).andThen(Single.just(it.address)) }
    }

    private class SafeNotDeployedException : Exception()

    companion object {
        const val DELAY_IN_SECONDS = 5L
    }
}
