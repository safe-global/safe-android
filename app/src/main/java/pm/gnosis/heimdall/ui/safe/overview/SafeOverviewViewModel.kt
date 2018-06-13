package pm.gnosis.heimdall.ui.safe.overview

import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.model.Solidity
import javax.inject.Inject

class SafeOverviewViewModel @Inject constructor(
    private val safeRepository: GnosisSafeRepository
) : SafeOverviewContract() {
    private val infoCache = mutableMapOf<Solidity.Address, SafeInfo>()

    override fun loadSafeInfo(address: Solidity.Address): Single<SafeInfo> =
        safeRepository.loadInfo(address).firstOrError()
            .doOnSuccess { infoCache[address] = it }
            .onErrorResumeNext { infoCache[address]?.let { Single.just(it) } ?: Single.error(it) }
}
