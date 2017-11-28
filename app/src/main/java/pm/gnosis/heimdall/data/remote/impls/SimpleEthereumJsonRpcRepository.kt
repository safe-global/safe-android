package pm.gnosis.heimdall.data.remote.impls

import io.reactivex.Observable
import pm.gnosis.heimdall.data.remote.*
import pm.gnosis.heimdall.data.remote.BulkRequest.SubRequest
import pm.gnosis.heimdall.data.remote.models.JsonRpcRequest
import pm.gnosis.heimdall.data.remote.models.TransactionCallParams
import pm.gnosis.heimdall.data.remote.models.TransactionParameters
import pm.gnosis.heimdall.data.remote.models.TransactionReceipt
import pm.gnosis.models.Wei
import pm.gnosis.utils.hexAsBigInteger
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleEthereumJsonRpcRepository @Inject constructor(
        private val ethereumJsonRpcApi: EthereumJsonRpcApi
) : EthereumJsonRpcRepository {

    override fun <R : BulkRequest> bulk(request: R): Observable<R> {
        return ethereumJsonRpcApi.post(request.body())
                .map { request.parse(it); request }
    }

    override fun getBalance(address: BigInteger): Observable<Wei> =
            ethereumJsonRpcApi.post(
                    JsonRpcRequest(
                            method = EthereumJsonRpcRepository.FUNCTION_GET_BALANCE,
                            params = arrayListOf(address, EthereumJsonRpcRepository.DEFAULT_BLOCK_LATEST)))
                    .map { Wei(it.checkedResult().hexAsBigInteger()) }

    override fun getLatestBlock(): Observable<BigInteger> =
            ethereumJsonRpcApi.post(JsonRpcRequest(method = "eth_blockNumber"))
                    .map { it.checkedResult().hexAsBigInteger() }

    override fun call(transactionCallParams: TransactionCallParams): Observable<String> =
            ethereumJsonRpcApi.post(JsonRpcRequest(method = "eth_call",
                    params = arrayListOf(transactionCallParams, EthereumJsonRpcRepository.DEFAULT_BLOCK_LATEST)))
                    .map { it.checkedResult() }

    override fun sendRawTransaction(signedTransactionData: String): Observable<String> =
            ethereumJsonRpcApi.post(JsonRpcRequest(method = "eth_sendRawTransaction",
                    params = arrayListOf(signedTransactionData)))
                    .map { it.checkedResult() }

    override fun getTransactionCount(address: BigInteger): Observable<BigInteger> =
            ethereumJsonRpcApi.post(JsonRpcRequest(method = "eth_getTransactionCount",
                    params = arrayListOf(address, EthereumJsonRpcRepository.DEFAULT_BLOCK_LATEST)))
                    .map { it.checkedResult().hexAsBigInteger() }

    override fun getTransactionReceipt(receiptHash: String): Observable<TransactionReceipt> =
            ethereumJsonRpcApi.receipt(JsonRpcRequest(method = "eth_getTransactionReceipt",
                    params = arrayListOf(receiptHash)))
                    .map { it.checkedResult() }

    override fun getGasPrice(): Observable<BigInteger> =
            ethereumJsonRpcApi.post(JsonRpcRequest(method = "eth_gasPrice"))
                    .map { it.checkedResult().hexAsBigInteger() }

    override fun estimateGas(transactionCallParams: TransactionCallParams): Observable<BigInteger> =
            ethereumJsonRpcApi.post(JsonRpcRequest(method = "eth_estimateGas",
                    params = arrayListOf(transactionCallParams)))
                    .doOnNext { Timber.d(it.toString()) }
                    .map { it.checkedResult().hexAsBigInteger() }

    class TransactionParametersRequest(val estimatedGas: SubRequest<BigInteger>, val gasPrice: SubRequest<BigInteger>, val transactionCount: SubRequest<BigInteger>) :
            BulkRequest(estimatedGas, gasPrice, transactionCount)

    override fun getTransactionParameters(address: BigInteger, transactionCallParams: TransactionCallParams): Observable<TransactionParameters> {
        val request = TransactionParametersRequest(
                SubRequest(JsonRpcRequest(id = 0, method = "eth_estimateGas", params = arrayListOf(transactionCallParams)), { it.checkedResult().hexAsBigInteger() }),
                SubRequest(JsonRpcRequest(id = 1, method = "eth_gasPrice"), { it.checkedResult().hexAsBigInteger() }),
                SubRequest(JsonRpcRequest(id = 2, method = "eth_getTransactionCount", params = arrayListOf(address, EthereumJsonRpcRepository.DEFAULT_BLOCK_LATEST)), { it.checkedResult().hexAsBigInteger() })
        )
        return bulk(request).map {
            val adjustedGas = BigDecimal.valueOf(1.1)
                    .multiply(BigDecimal(it.estimatedGas.value)).setScale(0, BigDecimal.ROUND_UP).unscaledValue()
            TransactionParameters(adjustedGas, it.gasPrice.value!!, it.transactionCount.value!!)
        }
    }

    private fun JsonRpcResult.checkedResult(): String {
        error?.let {
            throw EthereumJsonRpcApi.ErrorResultException(it.message)
        }
        return result
    }

    private fun JsonRpcTransactionReceiptResult.checkedResult(): TransactionReceipt {
        error?.let {
            throw EthereumJsonRpcApi.ErrorResultException(it.message)
        }
        return result
    }
}
