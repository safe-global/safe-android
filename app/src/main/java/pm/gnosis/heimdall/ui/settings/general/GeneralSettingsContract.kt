package pm.gnosis.heimdall.ui.settings.general

import android.arch.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.svalinn.common.utils.Result

abstract class GeneralSettingsContract : ViewModel() {
    abstract fun isFingerprintAvailable(): Boolean
    abstract fun clearFingerprintData(): Single<Result<Unit>>
}
