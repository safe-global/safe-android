package pm.gnosis.heimdall.data.remote

import com.gojuno.koptional.Optional
import io.reactivex.Observable
import pm.gnosis.heimdall.common.util.ERC20
import pm.gnosis.heimdall.data.model.TransactionCallParams
import pm.gnosis.heimdall.data.model.Wei
import java.math.BigInteger

interface EthereumJsonRpcRepository {

    fun getBalance(): Observable<Wei>

    fun getLatestBlock(): Observable<BigInteger>

    fun call(transactionCallParams: TransactionCallParams): Observable<String>

    fun sendRawTransaction(signedTransactionData: String): Observable<String>

    fun getTransactionCount(): Observable<BigInteger>

    fun getGasPrice(): Observable<BigInteger>

    fun getTokenName(contractAddress: BigInteger): Observable<Optional<String>>

    fun getTokenSymbol(contractAddress: BigInteger): Observable<Optional<String>>

    fun getTokenDecimals(contractAddress: BigInteger): Observable<Optional<BigInteger>>

    fun getTokenInfo(contractAddress: BigInteger): Observable<ERC20.Token>

    fun estimateGas(transactionCallParams: TransactionCallParams): Observable<BigInteger>

    fun getTransactionParameters(transactionCallParams: TransactionCallParams): Observable<TransactionParameters>

    data class TransactionParameters(val gas: BigInteger, val gasPrice: BigInteger, val nonce: BigInteger)
}