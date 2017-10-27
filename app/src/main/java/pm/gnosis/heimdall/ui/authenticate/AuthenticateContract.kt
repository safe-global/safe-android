package pm.gnosis.heimdall.ui.authenticate

import android.arch.lifecycle.ViewModel
import android.content.Intent
import io.reactivex.Observable
import pm.gnosis.heimdall.common.utils.Result


abstract class AuthenticateContract: ViewModel() {
    abstract fun checkResult(result: ActivityResults): Observable<Result<Intent>>

    data class ActivityResults(val requestCode: Int, val resultCode: Int, val data: Intent?)
}