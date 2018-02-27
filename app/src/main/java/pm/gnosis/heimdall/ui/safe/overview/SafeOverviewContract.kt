package pm.gnosis.heimdall.ui.safe.overview

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.utils.Result
import java.math.BigInteger

abstract class SafeOverviewContract : ViewModel() {
    abstract fun removeSafe(address: BigInteger): Completable
    abstract fun observeSafes(): Flowable<Result<Adapter.Data<AbstractSafe>>>
    abstract fun loadSafeInfo(address: BigInteger): Single<SafeInfo>
    abstract fun observeDeployedStatus(hash: String): Observable<String>
    abstract fun shouldShowLowBalanceView(): Observable<Result<Boolean>>
    abstract fun dismissHasLowBalance()

    companion object {
        val LOW_BALANCE_THRESHOLD = Wei.ether("0.001")
    }
}
