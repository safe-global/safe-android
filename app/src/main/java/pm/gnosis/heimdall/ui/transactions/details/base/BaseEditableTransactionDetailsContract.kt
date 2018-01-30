package pm.gnosis.heimdall.ui.transactions.details.base

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.models.Safe
import java.math.BigInteger


abstract class BaseEditableTransactionDetailsContract : ViewModel() {

    abstract fun observeSafe(safe: BigInteger): Observable<Safe>
}