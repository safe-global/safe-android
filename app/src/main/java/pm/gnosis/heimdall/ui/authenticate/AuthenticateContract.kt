package pm.gnosis.heimdall.ui.authenticate

import android.arch.lifecycle.ViewModel
import android.content.Intent
import io.reactivex.Observable
import pm.gnosis.svalinn.common.utils.Result

abstract class AuthenticateContract : ViewModel() {
    abstract fun checkResult(input: String): Observable<Result<Intent>>
}
