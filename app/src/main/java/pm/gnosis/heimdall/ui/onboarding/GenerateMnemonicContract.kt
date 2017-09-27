package pm.gnosis.heimdall.ui.onboarding

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Observable
import pm.gnosis.heimdall.common.util.Result

abstract class GenerateMnemonicContract : ViewModel() {
    abstract fun generateMnemonic(): Observable<Result<String>>
    abstract fun saveAccountWithMnemonic(mnemonic: String): Completable
}
