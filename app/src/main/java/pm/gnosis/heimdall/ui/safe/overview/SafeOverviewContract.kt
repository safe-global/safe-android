package pm.gnosis.heimdall.ui.safe.overview

import android.arch.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.model.Solidity

abstract class SafeOverviewContract : ViewModel() {
    abstract fun loadSafeInfo(address: Solidity.Address): Single<SafeInfo>
}
