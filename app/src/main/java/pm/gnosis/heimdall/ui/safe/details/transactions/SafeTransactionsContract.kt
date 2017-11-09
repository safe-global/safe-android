package pm.gnosis.heimdall.ui.safe.details.transactions

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.ui.base.Adapter
import java.math.BigInteger


abstract class SafeTransactionsContract: ViewModel() {
    abstract fun setup(address: BigInteger)

    abstract fun initTransaction(reload: Boolean): Single<Result<Int>>

    abstract fun observeTransaction(loadMoreEvents: Observable<Unit>): Observable<out Result<PaginatedTransactions>>

    data class PaginatedTransactions(val hasMore: Boolean, val data: Adapter.Data<String>)
}