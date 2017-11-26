package pm.gnosis.heimdall.ui.safe.overview

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Flowable
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.base.Adapter
import java.math.BigInteger

abstract class SafeOverviewContract : ViewModel() {
    abstract fun removeSafe(address: BigInteger): Completable
    abstract fun observeSafes(): Flowable<Result<Adapter.Data<AbstractSafe>>>
}
