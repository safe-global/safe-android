package pm.gnosis.heimdall.data.remote

import io.reactivex.Observable
import pm.gnosis.heimdall.data.remote.models.TransactionCallParams
import pm.gnosis.heimdall.data.remote.models.TransactionParameters
import pm.gnosis.heimdall.data.remote.models.TransactionReceipt
import pm.gnosis.models.Wei
import java.math.BigInteger

interface EthereumJsonRpcRepository {

    companion object {
        const val DEFAULT_BLOCK_EARLIEST = "earliest"
        const val DEFAULT_BLOCK_LATEST = "latest"
        const val DEFAULT_BLOCK_PENDING = "pending"

        const val FUNCTION_GET_BALANCE = "eth_getBalance"
    }

    fun <R : BulkRequest> bulk(request: R): Observable<R>

    fun getBalance(address: BigInteger): Observable<Wei>

    fun call(transactionCallParams: TransactionCallParams): Observable<String>

    fun sendRawTransaction(signedTransactionData: String): Observable<String>

    fun getTransactionReceipt(receiptHash: String): Observable<TransactionReceipt>

    fun getTransactionParameters(from: BigInteger, to: BigInteger, value: Wei? = null, data: String? = null): Observable<TransactionParameters>

}
