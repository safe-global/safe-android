package pm.gnosis.heimdall.data.repositories.impls

import android.app.Activity
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.data.remote.TxExecutorApi
import pm.gnosis.heimdall.data.remote.models.TxExecutionData
import pm.gnosis.heimdall.data.remote.models.TxExecutionVoucherData
import pm.gnosis.heimdall.data.repositories.BillingRepository
import pm.gnosis.heimdall.data.repositories.BillingRepository.PurchaseType.PRODUCT
import pm.gnosis.heimdall.data.repositories.TxExecutorRepository
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexStringToByteArray
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DefaultTxExecutorRepository @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val billingRepository: BillingRepository,
    private val executorApi: TxExecutorApi
) : TxExecutorRepository {

    override fun buyCredits(activity: Activity): Single<Boolean> =
        billingRepository.launchPurchaseFlow(activity, EXECUTOR_SERVICE_CREDITS_ID, PRODUCT)

    override fun loadBalance(): Single<Long> =
        authentication()
            .flatMapObservable { (account, signature) -> executorApi.balance(account, signature) }
            .map { it.balance }
            .firstOrError()

    /**
     * Returns a data result with the voucher id for a product if available or an error result otherwise
     */
    override fun observeVoucher(): Observable<Result<String>> =
        billingRepository.observePurchases()
            .map {
                it.find { it.productId == EXECUTOR_SERVICE_CREDITS_ID }?.token?.let {
                    DataResult(it)
                } ?: ErrorResult<String>(NoSuchElementException())
            }

    override fun redeemVoucher(voucher: String): Single<Long> =
        authentication()
            .flatMap { (account, signature) ->
                executorApi.reedeemVoucher(account, signature, TxExecutionVoucherData(voucher)).map { it.balance }
            }
            .onErrorResumeNext { throwable: Throwable ->
                if (throwable is HttpException && throwable.code() == 409) Single.just(-1)
                else Single.error(throwable)
            }
            .flatMap { balance ->
                billingRepository.consume(voucher).map { balance }
            }

    override fun execute(transaction: Transaction): Observable<String> =
        authentication()
            .flatMapObservable { (account, signature) ->
                executorApi.executeTx(
                    account,
                    signature,
                    TxExecutionData(
                        transaction.address.asEthereumAddressString(),
                        transaction.data
                    )
                )
            }
            .map { it.hash }

    override fun estimate(transaction: Transaction): Observable<Pair<Long, Long>> =
        authentication()
            .flatMapObservable { (account, signature) ->
                executorApi.estimateTx(
                    account, signature,
                    TxExecutionData(
                        transaction.address.asEthereumAddressString(),
                        transaction.data
                    )
                )
            }
            .map { it.balance to it.requiredCredits }

    private fun authentication(): Single<Pair<String, String>> =
        accountsRepository.loadActiveAccount()
            .flatMap {
                val addressString = it.address.asEthereumAddressString()
                accountsRepository.sign(Sha3Utils.keccak(addressString.hexStringToByteArray())).map { addressString to it.toString() }
            }

    companion object {
        private const val EXECUTOR_SERVICE_CREDITS_ID = "pm.gnosis.heimdall.dev.100_transaction_executions_rinkeby_1"
    }
}
