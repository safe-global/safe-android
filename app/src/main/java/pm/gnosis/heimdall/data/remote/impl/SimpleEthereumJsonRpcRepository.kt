package pm.gnosis.heimdall.data.remote.impl

import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Observable
import io.reactivex.functions.Function3
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.util.ERC20
import pm.gnosis.heimdall.data.model.JsonRpcRequest
import pm.gnosis.heimdall.data.model.TransactionCallParams
import pm.gnosis.heimdall.data.model.Wei
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
        private val ethereumJsonRpcApi: EthereumJsonRpcApi,
        private val accountsRepository: AccountsRepository
) : EthereumJsonRpcRepository {
    companion object {
        const val DEFAULT_BLOCK_EARLIEST = "earliest"
        const val DEFAULT_BLOCK_LATEST = "latest"
        const val DEFAULT_BLOCK_PENDING = "pending"
    }

    override fun getBalance(): Observable<Wei> =
            accountsRepository.loadActiveAccount().flatMapObservable {
                ethereumJsonRpcApi.post(
                        JsonRpcRequest(
                                method = "eth_getBalance",
                                params = arrayListOf(it.address, DEFAULT_BLOCK_LATEST)))
                        .map { Wei(it.result.hexAsBigInteger()) }
            }

    override fun getLatestBlock(): Observable<BigInteger> =
            ethereumJsonRpcApi.post(JsonRpcRequest(method = "eth_blockNumber"))
                    .map { it.result.hexAsBigInteger() }

    override fun call(transactionCallParams: TransactionCallParams): Observable<String> =
            ethereumJsonRpcApi.post(JsonRpcRequest(method = "eth_call",
                    params = arrayListOf(transactionCallParams, DEFAULT_BLOCK_LATEST)))
                    .map { it.result }

    override fun sendRawTransaction(signedTransactionData: String): Observable<String> =
            ethereumJsonRpcApi.post(JsonRpcRequest(method = "eth_sendRawTransaction",
                    params = arrayListOf(signedTransactionData)))
                    .map { it.result }

    override fun getTransactionCount(): Observable<BigInteger> =
            accountsRepository.loadActiveAccount().flatMapObservable {
                ethereumJsonRpcApi.post(JsonRpcRequest(method = "eth_getTransactionCount",
                        params = arrayListOf(it.address, DEFAULT_BLOCK_LATEST)))
                        .map { it.result.hexAsBigInteger() }
            }

    override fun getGasPrice(): Observable<BigInteger> =
            ethereumJsonRpcApi.post(JsonRpcRequest(method = "eth_gasPrice"))
                    .map { it.result.hexAsBigInteger() }

    override fun getTokenName(contractAddress: BigInteger): Observable<Optional<String>> =
            call(TransactionCallParams(to = contractAddress.asEthereumAddressString(), data = "0x${ERC20.NAME_METHOD_ID}"))
                    .map { it.hexAsBigIntegerOrNull()?.toAlfaNumericAscii()?.trim().toOptional() }

    override fun getTokenSymbol(contractAddress: BigInteger): Observable<Optional<String>> =
            call(TransactionCallParams(to = contractAddress.asEthereumAddressString(), data = "0x${ERC20.SYMBOL_METHOD_ID}"))
                    .map { it.hexAsBigIntegerOrNull()?.toAlfaNumericAscii()?.trim().toOptional() }

    override fun getTokenDecimals(contractAddress: BigInteger): Observable<Optional<BigInteger>> =
            call(TransactionCallParams(to = contractAddress.asEthereumAddressString(), data = "0x${ERC20.DECIMALS_METHOD_ID}"))
                    .map { it.hexAsBigIntegerOrNull().toOptional() }

    override fun getTokenInfo(contractAddress: BigInteger): Observable<ERC20Token> =
            Observable.zip(
                    getTokenName(contractAddress),
                    getTokenSymbol(contractAddress),
                    getTokenDecimals(contractAddress),
                    Function3 { name, symbol, decimals -> ERC20Token(contractAddress.asEthereumAddressString(), name.toNullable(), symbol.toNullable(), decimals.toNullable()) })

    override fun estimateGas(transactionCallParams: TransactionCallParams): Observable<BigInteger> =
            ethereumJsonRpcApi.post(JsonRpcRequest(method = "eth_estimateGas",
                    params = arrayListOf(transactionCallParams)))
                    .doOnNext { Timber.d(it.toString()) }
                    .map { it.result.hexAsBigInteger() }

    override fun getTransactionParameters(transactionCallParams: TransactionCallParams): Observable<EthereumJsonRpcRepository.TransactionParameters> =
            Observable.zip(
                    estimateGas(transactionCallParams),
                    getGasPrice(),
                    getTransactionCount(),
                    Function3<BigInteger, BigInteger, BigInteger, EthereumJsonRpcRepository.TransactionParameters> { gas, gasPrice, nonce -> EthereumJsonRpcRepository.TransactionParameters(gas, gasPrice, nonce) }
            )
}