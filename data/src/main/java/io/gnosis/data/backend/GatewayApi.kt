package io.gnosis.data.backend

import io.gnosis.data.BuildConfig
import io.gnosis.data.models.ChainInfo
import io.gnosis.data.models.Page
import io.gnosis.data.models.SafeInfo
import io.gnosis.data.models.assets.CoinBalances
import io.gnosis.data.models.assets.Collectible
import io.gnosis.data.models.transaction.*
import retrofit2.http.*
import java.math.BigInteger
import java.util.*

interface GatewayApi {

    @GET("/v1/balances/supported-fiat-codes")
    suspend fun loadSupportedCurrencies(): List<String>

    @GET("/v1/chains/{chainId}/safes/{address}")
    suspend fun getSafeInfo(
        @Path("chainId") chainId: BigInteger,
        @Path("address") address: String
    ): SafeInfo

    @GET("/v1/chains/{chainId}/safes/{address}/balances/{fiat}")
    suspend fun loadBalances(
        @Path("chainId") chainId: BigInteger,
        @Path("address") address: String,
        @Path("fiat") fiat: String = "usd"
    ): CoinBalances

    @GET("/v1/chains/{chainId}/transactions/{transactionId}")
    suspend fun loadTransactionDetails(
        @Path("chainId") chainId: BigInteger,
        @Path("transactionId") transactionId: String
    ): TransactionDetails

    @POST("/v1/chains/{chainId}/transactions/{safeTxHash}/confirmations")
    suspend fun submitConfirmation(
        @Path("chainId") chainId: BigInteger,
        @Path("safeTxHash") safeTxHash: String,
        @Body txConfirmationRequest: TransactionConfirmationRequest
    ): TransactionDetails

    @POST("/v1/chains/{chainId}/transactions/{safeAddress}/propose")
    suspend fun proposeTransaction(
        @Path("chainId") chainId: BigInteger,
        @Path("safeAddress") safeAddress: String,
        @Body multisigTransactionRequest: MultisigTransactionRequest
    )

    @GET("/v2/chains/{chainId}/safes/{safeAddress}/collectibles")
    suspend fun loadCollectibles(
        @Path("chainId") chainId: BigInteger,
        @Path("safeAddress") safeAddress: String
    ): Page<Collectible>

    @GET
    suspend fun loadCollectiblesPage(@Url pageLink: String): Page<Collectible>

    @GET("/v1/chains/{chainId}/safes/{address}/transactions/history")
    suspend fun loadTransactionsHistory(
        @Path("chainId") chainId: BigInteger,
        @Path("address") address: String,
        @Query("timezone_offset") timezoneOffset: Int = TimeZone.getDefault().getOffset(Date().time)
    ): Page<TxListEntry>

    @GET("/v1/chains/{chainId}/safes/{address}/transactions/queued")
    suspend fun loadTransactionsQueue(
        @Path("chainId") chainId: BigInteger,
        @Path("address") address: String,
        @Query("timezone_offset") timezoneOffset: Int = TimeZone.getDefault().getOffset(Date().time)
    ): Page<TxListEntry>

    @GET
    suspend fun loadTransactionsPage(@Url pageLink: String): Page<TxListEntry>

    @GET("/v1/chains")
    suspend fun loadChainInfo(): Page<ChainInfo>

    @GET("/v1/chains/{chainId}")
    suspend fun loadChainInfo(@Path("chainId") chainId: BigInteger): ChainInfo

    @GET
    suspend fun loadChainInfoPage(@Url pageLink: String): Page<ChainInfo>

    @POST("/v2/chains/{chainId}/safes/{address}/multisig-transactions/estimations")
    suspend fun estimateTransaction(
        @Path("chainId") chainId: BigInteger,
        @Path("address") address: String,
        @Body transactionEstimationRequest: TransactionEstimationRequest
    ): TransactionEstimation

    companion object {
        const val BASE_URL = BuildConfig.CLIENT_GATEWAY_URL
    }
}
