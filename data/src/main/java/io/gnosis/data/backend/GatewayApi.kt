package io.gnosis.data.backend

import io.gnosis.data.backend.dto.GateTransactionDto
import io.gnosis.data.models.Page
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface GatewayApi {

    @GET("transactions/{address}")
    suspend fun loadTransactions(@Path("address") address: String, @Query("next") next: String? = null): Page<GateTransactionDto>

    @GET
    suspend fun loadTransactionsPage(@Url pageLink: String): Page<GateTransactionDto>

    companion object {
        const val BASE_URL = "https://safe-client-gateway.herokuapp.com/"
    }
}
