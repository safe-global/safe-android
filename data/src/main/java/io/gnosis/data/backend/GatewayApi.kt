package io.gnosis.data.backend

import io.gnosis.data.BuildConfig
import io.gnosis.data.models.ChainInfo
import io.gnosis.data.models.Page
import io.gnosis.data.models.SafeInfo
import io.gnosis.data.models.assets.CoinBalances
import io.gnosis.data.models.assets.Collectible
import io.gnosis.data.models.transaction.MultisigTransactionRequest
import io.gnosis.data.models.transaction.TransactionConfirmationRequest
import io.gnosis.data.models.transaction.TransactionDetails
import io.gnosis.data.models.transaction.TxListEntry
import retrofit2.http.*
import java.util.*

interface GatewayApi {

    @GET("/{chainId}/v1/balances/supported-fiat-codes")
    suspend fun loadSupportedCurrencies(
        @Path("chainId") chainId: Int = 4
    ): List<String>

    @GET("/{chainId}/v1/safes/{address}")
    suspend fun getSafeInfo(
        @Path("chainId") chainId: Int = 4,
        @Path("address") address: String
    ): SafeInfo

    @GET("/{chainId}/v1/safes/{address}/balances/{fiat}")
    suspend fun loadBalances(
        @Path("chainId") chainId: Int = 4,
        @Path("address") address: String,
        @Path("fiat") fiat: String = "usd"
    ): CoinBalances

    @GET("{chainId}/v1/transactions/{transactionId}")
    suspend fun loadTransactionDetails(
        @Path("chainId") chainId: Int = 4,
        @Path("transactionId") transactionId: String
    ): TransactionDetails

    @POST("{chainId}/v1/transactions/{safeTxHash}/confirmations")
    suspend fun submitConfirmation(
        @Path("chainId") chainId: Int = 4,
        @Path("safeTxHash") safeTxHash: String,
        @Body txConfirmationRequest: TransactionConfirmationRequest
    ): TransactionDetails

    @POST("{chainId}/v1/transactions/{safeAddress}/propose")
    suspend fun proposeTransaction(
        @Path("chainId") chainId: Int = 4,
        @Path("safeAddress") safeAddress: String,
        @Body multisigTransactionRequest: MultisigTransactionRequest
    )

    @GET("{chainId}/v1/safes/{safeAddress}/collectibles")
    suspend fun loadCollectibles(
        @Path("chainId") chainId: Int = 4,
        @Path("safeAddress") safeAddress: String
    ): List<Collectible>

    // Unified endpoints
    @GET("{chainId}/v1/safes/{address}/transactions/history")
    suspend fun loadTransactionsHistory(
        @Path("chainId") chainId: Int = 4,
        @Path("address") address: String,
        @Query("timezone_offset") timezoneOffset: Int = TimeZone.getDefault().getOffset(Date().time)
    ): Page<TxListEntry>

    @GET("{chainId}/v1/safes/{address}/transactions/queued")
    suspend fun loadTransactionsQueue(
        @Path("chainId") chainId: Int = 4,
        @Path("address") address: String,
        @Query("timezone_offset") timezoneOffset: Int = TimeZone.getDefault().getOffset(Date().time)
    ): Page<TxListEntry>

    @GET
    suspend fun loadTransactionsPage(@Url pageLink: String): Page<TxListEntry>

    @GET("v1/chains")
    suspend fun loadChainInfo(): Page<ChainInfo>

    companion object {
        const val BASE_URL = BuildConfig.CLIENT_GATEWAY_URL
    }
}
