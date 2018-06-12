package pm.gnosis.heimdall.ui.safe.overview

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei

abstract class SafeOverviewContract : ViewModel() {
    abstract fun loadSafeInfo(address: Solidity.Address): Single<SafeInfo>
    abstract fun observeDeployStatus(hash: String): Observable<String>

    companion object {
        val LOW_BALANCE_THRESHOLD = Wei.ether("0.001")
    }
}
