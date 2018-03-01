package pm.gnosis.heimdall.data.repositories

import android.app.Activity
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.models.Transaction


interface TxExecutorRepository {
    fun hasPlan(): Single<Boolean>
    fun observePlan(): Observable<Boolean>
    fun buyPlan(activity: Activity): Single<Boolean>
    fun execute(transaction: Transaction): Observable<String>
}
