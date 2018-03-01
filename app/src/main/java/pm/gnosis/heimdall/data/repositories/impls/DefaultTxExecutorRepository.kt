package pm.gnosis.heimdall.data.repositories.impls

import android.app.Activity
import android.util.Log
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.remote.TxExecutorApi
import pm.gnosis.heimdall.data.remote.models.TxExecutionData
import pm.gnosis.heimdall.data.repositories.BillingRepository
import pm.gnosis.heimdall.data.repositories.TxExecutorRepository
import pm.gnosis.models.Transaction
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DefaultTxExecutorRepository @Inject constructor(
    private val billingRepository: BillingRepository,
    private val executorApi: TxExecutorApi
) : TxExecutorRepository {
    override fun hasPlan(): Single<Boolean> =
        billingRepository.checkSubscription(EXECUTOR_SERVICE_SUBSCRIPTION_ID)
            .map { it.toNullable() != null }

    override fun observePlan(): Observable<Boolean> =
        billingRepository.observePurchases()
            .map { it.contains(EXECUTOR_SERVICE_SUBSCRIPTION_ID) }

    override fun buyPlan(activity: Activity): Single<Boolean> =
        billingRepository.launchSubscribeFlow(activity, EXECUTOR_SERVICE_SUBSCRIPTION_ID)

    override fun execute(transaction: Transaction): Observable<String> =
        billingRepository.checkSubscription(EXECUTOR_SERVICE_SUBSCRIPTION_ID)
            .flatMapObservable {
                it.toNullable()?.let {
                    executorApi.executeTx(
                        it.token,
                        TxExecutionData(
                            transaction.address.asEthereumAddressString(),
                            transaction.data
                        )
                    )
                } ?: Observable.error(IllegalStateException("No subscription"))
            }
            .map { it.hash }

    companion object {
        private const val EXECUTOR_SERVICE_SUBSCRIPTION_ID =
            "pm.gnosis.heimdall.dev.transaction_execution_rinkeby_1"
    }
}