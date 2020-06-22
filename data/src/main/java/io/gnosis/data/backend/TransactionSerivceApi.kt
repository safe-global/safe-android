package io.gnosis.data.backend

import io.gnosis.data.BuildConfig
import io.gnosis.data.backend.dto.ServiceBalance
import io.gnosis.data.backend.dto.TransactionDto
import io.gnosis.data.models.Page
import retrofit2.http.GET
import retrofit2.http.Path

interface TransactionServiceApi {

    @GET("v1/safes/{address}/balances/usd/")
    suspend fun loadBalances(@Path("address") address: String): List<ServiceBalance>

    @GET("v1/safes/{address}/all-transactions/")
    suspend fun loadTransactions(@Path("address") address: String): Page<TransactionDto>

    companion object {
        const val BASE_URL = BuildConfig.TRANSACTION_SERVICE_URL
    }
}
