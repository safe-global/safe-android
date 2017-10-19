package pm.gnosis.heimdall.ui.onboarding

import android.arch.lifecycle.ViewModel
import android.content.Intent
import io.reactivex.Observable
import pm.gnosis.heimdall.common.utils.Result

abstract class RestoreAccountContract : ViewModel() {
    abstract fun saveAccountWithMnemonic(mnemonic: String): Observable<Result<Intent>>
}
