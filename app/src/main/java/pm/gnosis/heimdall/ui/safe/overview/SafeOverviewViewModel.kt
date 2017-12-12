package pm.gnosis.heimdall.ui.safe.overview

import io.reactivex.Flowable
import io.reactivex.Single
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeWithInfo
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.scanToAdapterData
import java.math.BigInteger
import javax.inject.Inject

class SafeOverviewViewModel @Inject constructor(
        private val accountsRepository: AccountsRepository,
        private val safeRepository: GnosisSafeRepository
) : SafeOverviewContract() {
    override fun observeSafes(): Flowable<Result<Adapter.Data<AbstractSafe>>> {
        return safeRepository.observeSafes()
                .map {
                    it.map {
                        if (it is Safe) {
                            SafeWithInfo(it)
                        } else it
                    }
                }
                .scanToAdapterData()
                .mapToResult()
    }

    override fun removeSafe(address: BigInteger) =
            safeRepository.remove(address)

    override fun loadAccountAddress(): Single<BigInteger> =
            accountsRepository.loadActiveAccount().map { it.address }
}
