package io.gnosis.data.backend

import io.gnosis.data.BuildConfig
import io.gnosis.data.backend.dto.SafeInfoDto
import retrofit2.http.GET
import retrofit2.http.Path

@Deprecated("use client gw")
interface TransactionServiceApi {

    @GET("v1/safes/{address}")
    suspend fun getSafeInfo(@Path("address") address: String): SafeInfoDto

    companion object {
        const val BASE_URL = BuildConfig.TRANSACTION_SERVICE_URL
    }
}
