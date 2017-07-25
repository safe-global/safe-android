package pm.gnosis.android.app.wallet.data.remote

import io.reactivex.Observable
import pm.gnosis.android.app.wallet.data.geth.GethAccountManager
import pm.gnosis.android.app.wallet.data.model.JsonRpcRequest
import pm.gnosis.android.app.wallet.data.model.TransactionCallParams
import pm.gnosis.android.app.wallet.data.model.Wei
import pm.gnosis.android.app.wallet.util.asHexString
import pm.gnosis.android.app.wallet.util.hexAsBigInteger
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InfuraRepository @Inject constructor(private val infuraApi: InfuraApi,
                                           private val gethAccountManager: GethAccountManager) {
    companion object {
        const val DEFAULT_BLOCK_EARLIEST = "earliest"
        const val DEFAULT_BLOCK_LATEST = "latest"
        const val DEFAULT_BLOCK_PENDING = "pending"
    }

    fun getBalance(): Observable<Wei> =
            infuraApi.post(
                    JsonRpcRequest(
                            method = "eth_getBalance",
                            params = arrayListOf(gethAccountManager.getAccount().address.hex, DEFAULT_BLOCK_LATEST)))
                    .map { Wei(it.result.hexAsBigInteger()) }

    fun getLatestBlock(): Observable<BigInteger> =
            infuraApi.post(JsonRpcRequest(method = "eth_blockNumber"))
                    .map { it.result.hexAsBigInteger() }

    fun sendTransaction(transactionCallParams: TransactionCallParams): Observable<BigInteger> =
            infuraApi.post(JsonRpcRequest(method = "eth_sendTransaction",
                    params = arrayListOf(transactionCallParams)))
                    .map { it.result.hexAsBigInteger() }

    fun sendRawTransaction(signedTransactionData: String): Observable<String> =
            infuraApi.post(JsonRpcRequest(method = "eth_sendRawTransaction",
                    params = arrayListOf(signedTransactionData)))
                    .map { it.result }

    fun getTransactionCount(): Observable<BigInteger> =
            infuraApi.post(JsonRpcRequest(method = "eth_getTransactionCount",
                    params = arrayListOf(gethAccountManager.getAccount().address.hex, DEFAULT_BLOCK_LATEST)))
                    .map { it.result.hexAsBigInteger() }

    fun getGasPrice(): Observable<BigInteger> =
            infuraApi.post(JsonRpcRequest(method = "eth_gasPrice"))
                    .map { it.result.hexAsBigInteger() }

    fun estimateGas(from: BigInteger, to: BigInteger, value: BigInteger, data: String): Observable<BigInteger> =
            infuraApi.post(JsonRpcRequest(method = "eth_estimateGas", id = 42,
                    params = arrayListOf(TransactionCallParams(from = from.asHexString(), to = to.asHexString(), value = value.asHexString(), data = data))))
                    .doOnNext { Timber.d(it.toString()) }
                    .map { it.result.hexAsBigInteger() }
}
