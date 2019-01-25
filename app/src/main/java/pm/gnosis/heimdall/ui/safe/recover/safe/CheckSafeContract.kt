package pm.gnosis.heimdall.ui.safe.recover.safe

import androidx.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.svalinn.common.utils.Result

abstract class CheckSafeContract : ViewModel() {
    abstract fun checkSafe(address: CharSequence): Single<Result<CheckResult>>

    enum class CheckResult {
        INVALID_SAFE,
        VALID_SAFE_WITHOUT_EXTENSION,
        VALID_SAFE_WITH_EXTENSION
    }
}
