package pm.gnosis.heimdall.data.repositories

import android.app.Activity
import com.gojuno.koptional.Optional
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.UserPurchase


interface BillingRepository {
    fun checkPurchase(id: String, type: PurchaseType): Single<Optional<UserPurchase>>
    fun init()
    fun launchPurchaseFlow(activity: Activity, productId: String, type: PurchaseType): Single<Boolean>
    fun observePurchases(): Observable<out Collection<UserPurchase>>
    fun consume(token: String): Single<Boolean>

    enum class PurchaseType {
        SUBSCRIPTION,
        PRODUCT
    }
}
