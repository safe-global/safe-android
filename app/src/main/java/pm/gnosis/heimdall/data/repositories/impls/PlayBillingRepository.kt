package pm.gnosis.heimdall.data.repositories.impls

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.SingleOnSubscribe
import io.reactivex.rxkotlin.subscribeBy
import pm.gnosis.heimdall.data.repositories.BillingRepository
import pm.gnosis.heimdall.data.repositories.models.UserPurchase
import pm.gnosis.heimdall.helpers.SetStore
import pm.gnosis.svalinn.common.di.ApplicationContext
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayBillingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : BillingRepository, PurchasesUpdatedListener {

    private val purchaseStore = SetStore<String>()

    private val billingClient = BillingClient.newBuilder(context).setListener(this).build()

    private val pendingActionsLock = Any()
    private val pendingActions = ArrayList<BillingAction>()

    private var isConnected = false

    override fun init() {
        updatePurchases()
    }

    private fun updatePurchases() {
        billingAction {
            purchaseStore.replaceAll(
                billingClient.queryPurchases(BillingClient.SkuType.SUBS).purchasesList
                    .map { it.sku }
            )
        }.subscribeBy(onError = Timber::e)
    }

    override fun checkSubscription(id: String): Single<Optional<UserPurchase>> =
        billingAction {
            billingClient.queryPurchases(BillingClient.SkuType.SUBS)
                .purchasesList
                .find { it.sku == id }
                ?.let { UserPurchase(it.purchaseToken, it.orderId) }
                .toOptional()
        }

    override fun launchSubscribeFlow(activity: Activity, productId: String): Single<Boolean> {
        val weakActivity = WeakReference(activity)
        return billingAction {
            weakActivity.get()?.let {
                billingClient.launchBillingFlow(
                    it, BillingFlowParams.newBuilder()
                        .setType(BillingClient.SkuType.SUBS)
                        .setSku(productId)
                        .build()
                )
                true
            } ?: run {
                // Default to error if no activity found
                false
            }
        }
    }

    override fun onPurchasesUpdated(billingResponse: Int, purchases: MutableList<Purchase>?) {
        updatePurchases()
    }

    override fun observePurchases() =
        purchaseStore.observe()

    private fun startServiceConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(@BillingClient.BillingResponse billingResponseCode: Int) {
                isConnected = billingResponseCode == BillingClient.BillingResponse.OK
                executePendingActions()
            }

            override fun onBillingServiceDisconnected() {
                isConnected = false
            }
        })
    }

    private fun executeServiceRequest(action: BillingAction) {
        synchronized(pendingActionsLock) {
            if (isConnected) {
                action.invoke()
            } else {
                pendingActions.add(action)
                startServiceConnection()
            }
        }
    }

    private fun executePendingActions() {
        synchronized(pendingActionsLock) {
            pendingActions.forEach {
                if (isConnected) {
                    it.invoke()
                } else {
                    it.error()
                }
            }
            pendingActions.clear()
        }
    }

    private fun <T> billingAction(action: () -> T) = Single.create(BillingActionSingle<T>(action))

    private interface BillingAction {
        fun invoke()
        fun error()
    }

    private inner class BillingActionSingle<T>(
        private val action: () -> T
    ) : SingleOnSubscribe<T>, BillingAction {

        private var emitter: SingleEmitter<T>? = null

        override fun subscribe(e: SingleEmitter<T>) {
            emitter = e
            e.setCancellable {
                synchronized(pendingActionsLock) {
                    pendingActions.remove(this)
                }
                this.emitter = null
            }
            executeServiceRequest(this)
        }

        override fun invoke() {
            emitter?.apply {
                if (!isDisposed) {
                    try {
                        onSuccess(action())
                    } catch (e: Exception) {
                        onError(e)
                    }
                }
            }
        }

        override fun error() {
            emitter?.apply {
                if (!isDisposed) {
                    onError(IllegalStateException())
                }
            }
        }
    }
}
