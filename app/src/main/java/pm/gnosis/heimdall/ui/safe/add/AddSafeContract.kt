package pm.gnosis.heimdall.ui.safe.add

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.models.Wei
import pm.gnosis.ticker.data.repositories.models.Currency
import java.math.BigDecimal


abstract class AddSafeContract : ViewModel() {
    abstract fun addExistingSafe(name: String, address: String): Observable<Result<Unit>>

    abstract fun deployNewSafe(name: String): Observable<Result<Unit>>

    abstract fun observeEstimate(): Observable<Wei>

    abstract fun loadFiatConversion(wei: Wei): Single<Result<Pair<BigDecimal, Currency>>>
}
