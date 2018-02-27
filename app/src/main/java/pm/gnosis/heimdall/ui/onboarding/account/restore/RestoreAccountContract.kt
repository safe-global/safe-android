package pm.gnosis.heimdall.ui.onboarding.account.restore

import android.arch.lifecycle.ViewModel
import android.content.Intent
import io.reactivex.Observable
import pm.gnosis.svalinn.common.utils.Result

abstract class RestoreAccountContract : ViewModel() {
    abstract fun saveAccountWithMnemonic(mnemonic: String): Observable<Result<Intent>>
}
