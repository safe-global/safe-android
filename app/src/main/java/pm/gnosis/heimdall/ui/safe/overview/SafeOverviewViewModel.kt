package pm.gnosis.heimdall.ui.safe.overview

import io.reactivex.Flowable
import io.reactivex.Single
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.scanToAdapterData
import java.math.BigInteger
import javax.inject.Inject

class SafeOverviewViewModel @Inject constructor(
        private val safeRepository: GnosisSafeRepository
) : SafeOverviewContract() {
    private val infoCache = mutableMapOf<BigInteger, SafeInfo>()

    override fun observeSafes(): Flowable<Result<Adapter.Data<AbstractSafe>>> {
        return safeRepository.observeSafes()
                .scanToAdapterData()
                .mapToResult()
    }

    override fun removeSafe(address: BigInteger) =
            safeRepository.remove(address)

    override fun loadSafeInfo(address: BigInteger): Single<SafeInfo> =
            infoCache[address]?.let { Single.just(it) } ?:
                    safeRepository.loadInfo(address).firstOrError()
                            .doOnSuccess { infoCache.put(address, it) }

    override fun observeDeployedStatus(hash: String) =
            safeRepository.observeDeployStatus(hash)
}
