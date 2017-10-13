package pm.gnosis.heimdall.data.remote

import io.reactivex.Observable
import pm.gnosis.heimdall.data.model.TransactionCallParams
import pm.gnosis.heimdall.data.model.Wei
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import java.math.BigInteger

interface EthereumJsonRpcRepository {

    companion object {
        const val DEFAULT_BLOCK_EARLIEST = "earliest"
        const val DEFAULT_BLOCK_LATEST = "latest"
        const val DEFAULT_BLOCK_PENDING = "pending"

        const val FUNCTION_GET_BALANCE = "eth_getBalance"
    }

    fun <R : BulkRequest> bulk(request: R): Observable<R>

    fun getBalance(address: String): Observable<Wei>

    fun getLatestBlock(): Observable<BigInteger>

    fun call(transactionCallParams: TransactionCallParams): Observable<String>

    fun sendRawTransaction(signedTransactionData: String): Observable<String>

    fun getTransactionCount(address: String): Observable<BigInteger>

    fun getGasPrice(): Observable<BigInteger>

    fun getTokenInfo(contractAddress: BigInteger): Observable<ERC20Token>

    fun estimateGas(transactionCallParams: TransactionCallParams): Observable<BigInteger>

    fun getTransactionParameters(address: String, transactionCallParams: TransactionCallParams): Observable<TransactionParameters>

    data class TransactionParameters(val gas: BigInteger, val gasPrice: BigInteger, val nonce: BigInteger)
}