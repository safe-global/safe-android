package pm.gnosis.heimdall.data.remote.impl

import io.reactivex.Observable
import pm.gnosis.heimdall.common.util.ERC20
import pm.gnosis.heimdall.data.model.JsonRpcRequest
import pm.gnosis.heimdall.data.model.TransactionCallParams
import pm.gnosis.heimdall.data.model.Wei
import pm.gnosis.heimdall.data.remote.BulkRequest
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcApi
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.hexAsBigIntegerOrNull
import pm.gnosis.utils.toAlfaNumericAscii
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleEthereumJsonRpcRepository @Inject constructor(
        private val ethereumJsonRpcApi: EthereumJsonRpcApi
) : EthereumJsonRpcRepository {

    override fun <R: BulkRequest> bulk(request: R): Observable<R> {
        return ethereumJsonRpcApi.post(request.body())
                .map { request.parse(it); request }
    }

    override fun getBalance(address: String): Observable<Wei> =
                ethereumJsonRpcApi.post(
                        JsonRpcRequest(
                                method = EthereumJsonRpcRepository.FUNCTION_GET_BALANCE,
                                params = arrayListOf(address, EthereumJsonRpcRepository.DEFAULT_BLOCK_LATEST)))
                        .map { Wei(it.result.hexAsBigInteger()) }

    override fun getLatestBlock(): Observable<BigInteger> =
            ethereumJsonRpcApi.post(JsonRpcRequest(method = "eth_blockNumber"))
                    .map { it.result.hexAsBigInteger() }

    override fun call(transactionCallParams: TransactionCallParams): Observable<String> =
            ethereumJsonRpcApi.post(JsonRpcRequest(method = "eth_call",
                    params = arrayListOf(transactionCallParams, EthereumJsonRpcRepository.DEFAULT_BLOCK_LATEST)))
                    .map { it.result }

    override fun sendRawTransaction(signedTransactionData: String): Observable<String> =
            ethereumJsonRpcApi.post(JsonRpcRequest(method = "eth_sendRawTransaction",
                    params = arrayListOf(signedTransactionData)))
                    .map { it.result }

    override fun getTransactionCount(address: String): Observable<BigInteger> =
            ethereumJsonRpcApi.post(JsonRpcRequest(method = "eth_getTransactionCount",
                        params = arrayListOf(address, EthereumJsonRpcRepository.DEFAULT_BLOCK_LATEST)))
                        .map { it.result.hexAsBigInteger() }

    override fun getGasPrice(): Observable<BigInteger> =
            ethereumJsonRpcApi.post(JsonRpcRequest(method = "eth_gasPrice"))
                    .map { it.result.hexAsBigInteger() }

    class TokenInfoRequest(val name: BulkRequest.Call<String?>, val symbol: BulkRequest.Call<String?>, val decimals: BulkRequest.Call<BigInteger?>):
            BulkRequest(name, symbol, decimals)

    override fun getTokenInfo(contractAddress: BigInteger): Observable<ERC20Token> {
        val request = TokenInfoRequest(
                BulkRequest.Call(TransactionCallParams(to = contractAddress.asEthereumAddressString(), data = "0x${ERC20.NAME_METHOD_ID}").callRequest(0),
                        { it.result.hexAsBigIntegerOrNull()?.toAlfaNumericAscii()?.trim() } ),
                BulkRequest.Call(TransactionCallParams(to = contractAddress.asEthereumAddressString(), data = "0x${ERC20.SYMBOL_METHOD_ID}").callRequest(1),
                        { it.result.hexAsBigIntegerOrNull()?.toAlfaNumericAscii()?.trim() } ),
                BulkRequest.Call(TransactionCallParams(to = contractAddress.asEthereumAddressString(), data = "0x${ERC20.DECIMALS_METHOD_ID}").callRequest(2),
                        { it.result.hexAsBigIntegerOrNull() } )
        )
        return bulk(request).map { ERC20Token(contractAddress.asEthereumAddressString(), it.name.value, it.symbol.value, it.decimals.value)}
    }

    override fun estimateGas(transactionCallParams: TransactionCallParams): Observable<BigInteger> =
            ethereumJsonRpcApi.post(JsonRpcRequest(method = "eth_estimateGas",
                    params = arrayListOf(transactionCallParams)))
                    .doOnNext { Timber.d(it.toString()) }
                    .map { it.result.hexAsBigInteger() }

    class TransactionParametersRequest(val estimatedGas: BulkRequest.Call<BigInteger>, val gasPrice: BulkRequest.Call<BigInteger>, val transactionCount: BulkRequest.Call<BigInteger>):
            BulkRequest(estimatedGas, gasPrice, transactionCount)

    override fun getTransactionParameters(address: String, transactionCallParams: TransactionCallParams): Observable<EthereumJsonRpcRepository.TransactionParameters> {
        val request = TransactionParametersRequest(
            BulkRequest.Call(JsonRpcRequest(id = 0, method = "eth_estimateGas", params = arrayListOf(transactionCallParams)), { it.result.hexAsBigInteger() } ),
            BulkRequest.Call(JsonRpcRequest(id = 1, method = "eth_gasPrice"), { it.result.hexAsBigInteger() } ),
            BulkRequest.Call(JsonRpcRequest(id = 2, method = "eth_getTransactionCount", params = arrayListOf(address, EthereumJsonRpcRepository.DEFAULT_BLOCK_LATEST)), { it.result.hexAsBigInteger() } )
        )
        return bulk(request).map { EthereumJsonRpcRepository.TransactionParameters(it.estimatedGas.value!!, it.gasPrice.value!!, it.transactionCount.value!!)}
    }
}