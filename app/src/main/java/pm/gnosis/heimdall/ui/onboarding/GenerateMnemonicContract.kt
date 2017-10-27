package pm.gnosis.heimdall.ui.onboarding

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Single
import pm.gnosis.heimdall.common.utils.Result

abstract class GenerateMnemonicContract : ViewModel() {
    abstract fun generateMnemonic(): Single<Result<String>>
    abstract fun saveAccountWithMnemonic(mnemonic: String): Completable
}
