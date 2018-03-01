package pm.gnosis.heimdall.data.repositories

import android.app.Activity
import com.gojuno.koptional.Optional
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.UserPurchase


interface BillingRepository {
    fun checkSubscription(id: String): Single<Optional<UserPurchase>>
    fun init()
    fun launchSubscribeFlow(activity: Activity, productId: String): Single<Boolean>
    fun observePurchases(): Observable<Set<String>>
}
