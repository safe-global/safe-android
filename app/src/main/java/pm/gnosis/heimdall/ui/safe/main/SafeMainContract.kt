package pm.gnosis.heimdall.ui.safe.main

import android.arch.lifecycle.ViewModel
import io.reactivex.Flowable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.svalinn.common.utils.Result
import java.math.BigInteger

abstract class SafeMainContract : ViewModel() {
    abstract fun loadSelectedSafe(): Single<out AbstractSafe>
    abstract fun observeSafes(): Flowable<Result<Adapter.Data<AbstractSafe>>>
    abstract fun selectSafe(addressOrHash: BigInteger): Single<out AbstractSafe>
}
