package pm.gnosis.heimdall.ui.onboarding.account.create

import android.arch.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.heimdall.common.utils.Result

abstract class GenerateMnemonicContract : ViewModel() {
    abstract fun saveAccountWithMnemonic(mnemonic: String): Single<Result<Unit>>
    abstract fun generateMnemonic(): Single<Result<String>>
}
