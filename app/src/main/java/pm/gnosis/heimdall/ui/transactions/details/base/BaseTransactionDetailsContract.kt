package pm.gnosis.heimdall.ui.transactions.details.base

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.models.Safe
import java.math.BigInteger

abstract class BaseTransactionDetailsContract : ViewModel() {
    abstract fun observeSafe(safeAddress: BigInteger): Observable<Safe>
}
