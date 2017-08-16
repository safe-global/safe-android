package pm.gnosis.android.app.authenticator.data.remote

import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Observable
import io.reactivex.functions.Function3
import pm.gnosis.android.app.authenticator.data.geth.GethAccountManager
import pm.gnosis.android.app.authenticator.data.model.JsonRpcRequest
import pm.gnosis.android.app.authenticator.data.model.TransactionCallParams
import pm.gnosis.android.app.authenticator.data.model.Wei
import pm.gnosis.android.app.authenticator.util.*
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
                            params = arrayListOf(gethAccountManager.getActiveAccount().address.hex, DEFAULT_BLOCK_LATEST)))
                    .map { Wei(it.result.hexAsBigInteger()) }

    fun getLatestBlock(): Observable<BigInteger> =
            infuraApi.post(JsonRpcRequest(method = "eth_blockNumber"))
                    .map { it.result.hexAsBigInteger() }

    fun call(transactionCallParams: TransactionCallParams): Observable<String> =
            infuraApi.post(JsonRpcRequest(method = "eth_call",
                    params = arrayListOf(transactionCallParams, DEFAULT_BLOCK_LATEST)))
                    .map { it.result }

    fun sendRawTransaction(signedTransactionData: String): Observable<String> =
            infuraApi.post(JsonRpcRequest(method = "eth_sendRawTransaction",
                    params = arrayListOf(signedTransactionData)))
                    .map { it.result }

    fun getTransactionCount(): Observable<BigInteger> =
            infuraApi.post(JsonRpcRequest(method = "eth_getTransactionCount",
                    params = arrayListOf(gethAccountManager.getActiveAccount().address.hex, DEFAULT_BLOCK_LATEST)))
                    .map { it.result.hexAsBigInteger() }

    fun getGasPrice(): Observable<BigInteger> =
            infuraApi.post(JsonRpcRequest(method = "eth_gasPrice"))
                    .map { it.result.hexAsBigInteger() }

    fun getTokenName(contractAddress: BigInteger): Observable<Optional<String>> =
            call(TransactionCallParams(to = contractAddress.asEthereumAddressString(), data = "0x${ERC20.NAME_METHOD_ID}"))
                    .map { it.hexAsBigIntegerOrNull()?.toAlfaNumericAscii()?.trim().toOptional() }

    fun getTokenSymbol(contractAddress: BigInteger): Observable<Optional<String>> =
            call(TransactionCallParams(to = contractAddress.asEthereumAddressString(), data = "0x${ERC20.SYMBOL_METHOD_ID}"))
                    .map { it.hexAsBigIntegerOrNull()?.toAlfaNumericAscii()?.trim().toOptional() }

    fun getTokenDecimals(contractAddress: BigInteger): Observable<Optional<BigInteger>> =
            call(TransactionCallParams(to = contractAddress.asEthereumAddressString(), data = "0x${ERC20.DECIMALS_METHOD_ID}"))
                    .map { it.hexAsBigIntegerOrNull().toOptional() }

    fun getTokenInfo(contractAddress: BigInteger): Observable<ERC20.Token> =
            Observable.zip(
                    getTokenName(contractAddress),
                    getTokenSymbol(contractAddress),
                    getTokenDecimals(contractAddress),
                    Function3 { name, symbol, decimals -> ERC20.Token(contractAddress.asEthereumAddressString(), name.toNullable(), symbol.toNullable(), decimals.toNullable()) })

    fun estimateGas(transactionCallParams: TransactionCallParams): Observable<BigInteger> =
            infuraApi.post(JsonRpcRequest(method = "eth_estimateGas",
                    params = arrayListOf(transactionCallParams)))
                    .doOnNext { Timber.d(it.toString()) }
                    .map { it.result.hexAsBigInteger() }

    fun getTransactionParameters(transactionCallParams: TransactionCallParams): Observable<TransactionParameters> =
            Observable.zip(
                    estimateGas(transactionCallParams),
                    getGasPrice(),
                    getTransactionCount(),
                    Function3<BigInteger, BigInteger, BigInteger, TransactionParameters> { gas, gasPrice, nonce -> TransactionParameters(gas, gasPrice, nonce) }
            )

    data class TransactionParameters(val gas: BigInteger, val gasPrice: BigInteger, val nonce: BigInteger)
}
