package pm.gnosis.heimdall.ui.safe.main

import androidx.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result

abstract class SafeMainContract : ViewModel() {
    abstract fun loadSelectedSafe(): Single<out AbstractSafe>
    abstract fun observeSafes(): Flowable<Result<Adapter.Data<AbstractSafe>>>
    abstract fun selectSafe(address: Solidity.Address): Single<out AbstractSafe>
    abstract fun syncWithChromeExtension(address: Solidity.Address): Completable
    abstract fun updateSafeName(safe: AbstractSafe, name: String?): Completable
    abstract fun observeSafe(safe: AbstractSafe): Flowable<Pair<String, String>>
    abstract fun removeSafe(safe: AbstractSafe): Completable
    abstract fun loadSafeConfig(safe: AbstractSafe): Single<Result<Pair<Solidity.Address?, Boolean>>>
    abstract fun shouldShowWalletConnectIntro(): Single<Boolean>
}
