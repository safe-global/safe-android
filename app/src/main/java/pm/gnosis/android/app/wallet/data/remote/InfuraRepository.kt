package pm.gnosis.android.app.wallet.data.remote

import io.reactivex.Observable
import pm.gnosis.android.app.wallet.data.GethAccountManager
import pm.gnosis.android.app.wallet.data.model.JsonRpcRequest
import pm.gnosis.android.app.wallet.data.model.Wei
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
                    .map { Wei(BigInteger(it.result.removePrefix("0x"), 16)) }

    fun getLatestBlock(): Observable<BigInteger> =
            infuraApi.post(JsonRpcRequest(method = "eth_blockNumber"))
                    .map { BigInteger(it.result.removePrefix("0x"), 16) }

    fun sendRawTransaction(signedTransactionData: String): Observable<String> =
            infuraApi.post(JsonRpcRequest(method = "eth_sendRawTransaction",
                    params = arrayListOf(signedTransactionData)))
                    .map { it.result }

    fun getTransactionCount(): Observable<BigInteger> =
            infuraApi.post(JsonRpcRequest(method = "eth_getTransactionCount",
                    params = arrayListOf(gethAccountManager.getAccount().address.hex, DEFAULT_BLOCK_LATEST)))
                    .map { BigInteger(it.result.removePrefix("0x")) }
}
