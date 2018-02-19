package pm.gnosis.heimdall.ui.account

import android.arch.lifecycle.ViewModel
import android.graphics.Bitmap
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.accounts.base.models.Account
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.models.Wei
import pm.gnosis.ticker.data.repositories.models.Currency
import java.math.BigDecimal

abstract class AccountContract : ViewModel() {
    abstract fun getAccountBalance(): Observable<Result<Wei>>
    abstract fun getAccountAddress(): Observable<Result<Account>>
    abstract fun getQrCode(contents: String): Single<Result<Bitmap>>
    abstract fun loadFiatConversion(amount: Wei): Single<Result<Pair<BigDecimal, Currency>>>
}
