package pm.gnosis.heimdall.ui.safe.overview

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.scanToAdapterData
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.svalinn.common.utils.mapToResult
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SafeOverviewViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val ethereumRepository: EthereumRepository,
    private val preferencesManager: PreferencesManager,
    private val safeRepository: GnosisSafeRepository
) : SafeOverviewContract() {
    private val infoCache = mutableMapOf<Solidity.Address, SafeInfo>()

    override fun observeSafes(): Flowable<Result<Adapter.Data<AbstractSafe>>> = safeRepository.observeSafes()
        .scanToAdapterData()
        .mapToResult()

    override fun removeSafe(address: Solidity.Address) = safeRepository.removeSafe(address)

    override fun loadSafeInfo(address: Solidity.Address): Single<SafeInfo> =
        safeRepository.loadInfo(address).firstOrError()
            .doOnSuccess { infoCache[address] = it }
            .onErrorResumeNext { infoCache[address]?.let { Single.just(it) } ?: Single.error(it) }

    override fun observeDeployStatus(hash: String) = safeRepository.observeDeployStatus(hash)

    override fun shouldShowLowBalanceView(): Observable<Result<Boolean>> = hasLowBalance()
        .map { it && !preferencesManager.prefs.getBoolean(PreferencesManager.DISMISS_LOW_BALANCE, false) }
        .mapToResult()
        .repeatWhen { it.delay(BALANCE_CHECK_TIME_INTERVAL_SECONDS, TimeUnit.SECONDS) }
        .toObservable()

    private fun hasLowBalance(): Single<Boolean> = accountsRepository.loadActiveAccount()
        .flatMap { ethereumRepository.getBalance(it.address).firstOrError() }
        .map { it < LOW_BALANCE_THRESHOLD }
        // As soon as we get a higher balance response we reset the flag
        .doOnSuccess { hasLowBalance -> if (!hasLowBalance) setDismissLowBalance(false) }

    override fun dismissHasLowBalance() = setDismissLowBalance(true)

    private fun setDismissLowBalance(dismiss: Boolean) = preferencesManager.prefs.edit {
        putBoolean(PreferencesManager.DISMISS_LOW_BALANCE, dismiss)
    }

    companion object {
        private const val BALANCE_CHECK_TIME_INTERVAL_SECONDS = 10L
    }
}
