package pm.gnosis.heimdall.ui.onboarding.account.restore

import android.arch.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.svalinn.common.utils.Result

abstract class RestoreAccountContract : ViewModel() {
    abstract fun saveAccountWithMnemonic(mnemonic: String): Single<Result<Unit>>
}
