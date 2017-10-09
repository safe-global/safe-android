package pm.gnosis.heimdall.ui.onboarding

import android.arch.lifecycle.ViewModel
import android.content.Intent
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.mnemonic.Bip39ValidationResult

abstract class RestoreAccountContract : ViewModel() {
    abstract fun saveAccountWithMnemonic(mnemonic: String): Observable<Result<Intent>>
}
