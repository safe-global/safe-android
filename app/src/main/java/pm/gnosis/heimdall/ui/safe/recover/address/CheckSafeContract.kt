package pm.gnosis.heimdall.ui.safe.recover.address

import android.arch.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.svalinn.common.utils.Result

abstract class CheckSafeContract : ViewModel() {
    abstract fun checkSafe(address: CharSequence): Single<Result<Boolean>>
}
