package pm.gnosis.heimdall.ui.account

import android.arch.lifecycle.ViewModel
import android.graphics.Bitmap
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.common.utils.Result

abstract class AccountContract : ViewModel() {
    abstract fun getAccountBalance(): Observable<Result<Wei>>
    abstract fun getAccountAddress(): Observable<Result<Account>>
    abstract fun getQrCode(contents: String): Single<Result<Bitmap>>
}
