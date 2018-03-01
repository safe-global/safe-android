package pm.gnosis.heimdall.ui.settings.security.changepassword

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import pm.gnosis.svalinn.common.utils.Result

abstract class ChangePasswordContract : ViewModel() {
    abstract fun setPassword(currentPassword: String, newPassword: String, newPasswordRepeat: String): Observable<Result<Unit>>
}
