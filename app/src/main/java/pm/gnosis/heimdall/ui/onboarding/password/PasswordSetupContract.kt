package pm.gnosis.heimdall.ui.onboarding.password

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import pm.gnosis.heimdall.common.utils.Result

abstract class PasswordSetupContract : ViewModel() {
    abstract fun setPassword(password: String, repeat: String): Observable<Result<Unit>>
}
