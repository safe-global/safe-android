package pm.gnosis.heimdall.ui.safe.add

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.GasEstimate
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.ticker.data.repositories.models.Currency
import java.math.BigDecimal
import java.math.BigInteger

abstract class AddSafeContract : ViewModel() {
    abstract fun addExistingSafe(name: String, address: String): Observable<Result<Unit>>

    abstract fun deployNewSafe(name: String, overrideGasPrice: Wei?): Observable<Result<Unit>>

    abstract fun observeEstimate(): Observable<Result<GasEstimate>>

    abstract fun loadFiatConversion(wei: Wei): Single<Result<Pair<BigDecimal, Currency>>>

    abstract fun addAdditionalOwner(input: String): Observable<Result<Unit>>

    abstract fun removeAdditionalOwner(address: BigInteger): Observable<Result<Unit>>

    abstract fun setupDeploy(): Single<BigInteger>

    abstract fun observeAdditionalOwners(): Observable<List<BigInteger>>

    abstract fun loadSafeInfo(address: String): Observable<Result<SafeInfo>>

    abstract fun loadActiveAccount(): Observable<Account>
}
