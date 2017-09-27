package pm.gnosis.heimdall.ui.onboarding

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Single

abstract class RestoreAccountContract : ViewModel() {
    abstract fun isValidMnemonic(mnemonic: String): Single<Boolean>
    abstract fun saveAccountWithMnemonic(mnemonic: String): Completable
}
