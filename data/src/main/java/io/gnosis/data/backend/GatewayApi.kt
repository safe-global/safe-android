package io.gnosis.data.backend

import io.gnosis.data.backend.dto.GateTransactionDto
import io.gnosis.data.models.Page
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

interface GatewayApi {

    @GET("v1/safes/{address}/transactions")
    suspend fun loadTransactions(@Path("address") address: String): Page<GateTransactionDto>

    @GET
    suspend fun loadTransactionsPage(@Url pageLink: String): Page<GateTransactionDto>

    companion object {
        const val BASE_URL = "https://safe-client-gateway.herokuapp.com/v1/"
    }
}
