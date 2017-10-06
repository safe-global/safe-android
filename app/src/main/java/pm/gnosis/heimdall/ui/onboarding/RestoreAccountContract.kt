package pm.gnosis.heimdall.ui.onboarding

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Single
import pm.gnosis.mnemonic.Bip39ValidationResult

abstract class RestoreAccountContract : ViewModel() {
    abstract fun isValidMnemonic(mnemonic: String): Single<Bip39ValidationResult>
    abstract fun saveAccountWithMnemonic(mnemonic: String): Completable
}
