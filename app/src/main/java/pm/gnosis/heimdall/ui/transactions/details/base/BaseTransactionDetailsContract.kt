package pm.gnosis.heimdall.ui.transactions.details.base

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.model.Solidity
import java.math.BigInteger

abstract class BaseTransactionDetailsContract : ViewModel() {
    abstract fun observeSafe(safeAddress: Solidity.Address): Observable<Safe>
}
