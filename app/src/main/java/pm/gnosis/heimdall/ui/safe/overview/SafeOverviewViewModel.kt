package pm.gnosis.heimdall.ui.safe.overview

import io.reactivex.Flowable
import io.reactivex.Single
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.PreferencesManager
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.edit
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.scanToAdapterData
import java.math.BigInteger
import javax.inject.Inject

class SafeOverviewViewModel @Inject constructor(
        private val accountsRepository: AccountsRepository,
        private val ethereumJsonRpcRepository: EthereumJsonRpcRepository,
        private val preferencesManager: PreferencesManager,
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
            safeRepository.loadInfo(address).firstOrError()
                    .doOnSuccess { infoCache.put(address, it) }
                    .onErrorResumeNext {
                        infoCache[address]?.let { Single.just(it) } ?: Single.error(it)
                    }

    override fun observeDeployedStatus(hash: String) =
            safeRepository.observeDeployStatus(hash)

    override fun shouldShowLowBalanceView(): Single<Boolean> = hasLowBalance()
            .map { it && !preferencesManager.prefs.getBoolean(PreferencesManager.DISMISS_LOW_BALANCE, false) }

    private fun hasLowBalance(): Single<Boolean> = accountsRepository.loadActiveAccount()
            .flatMap { ethereumJsonRpcRepository.getBalance(it.address).firstOrError() }
            .map { it.toEther() < LOW_BALANCE_THRESHOLD }
            // As soon as we get a higher balance response we reset the flag
            .doOnSuccess { hasLowBalance -> if (!hasLowBalance) setDismissLowBalance(false) }

    override fun dismissHasLowBalance() {
        setDismissLowBalance(true)
    }

    private fun setDismissLowBalance(dismiss: Boolean) {
        preferencesManager.prefs.edit {
            putBoolean(PreferencesManager.DISMISS_LOW_BALANCE, dismiss)
        }
    }
}
