package pm.gnosis.heimdall.data.repositories

import android.app.Activity
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.common.utils.Result


interface TxExecutorRepository {
    fun buyCredits(activity: Activity): Single<Boolean>
    fun execute(transaction: Transaction): Observable<String>
    fun estimate(transaction: Transaction): Observable<Pair<Long, Long>>
    fun loadBalance(): Single<Long>
    fun observeVoucher(): Observable<Result<String>>
    fun redeemVoucher(voucher: String): Single<Long>
}
