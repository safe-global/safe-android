package io.gnosis.data.backend

import io.gnosis.data.BuildConfig
import io.gnosis.data.backend.dto.CollectibleDto
import io.gnosis.data.backend.dto.SafeInfoDto
import io.gnosis.data.backend.dto.ServiceBalance
import io.gnosis.data.backend.dto.ServiceTokenInfo
import retrofit2.http.GET
import retrofit2.http.Path

interface TransactionServiceApi {

    @GET("v1/safes/{address}/balances/usd/")
    suspend fun loadBalances(@Path("address") address: String): List<ServiceBalance>

    @GET("v1/safes/{address}/collectibles/")
    suspend fun loadCollectibles(@Path("address") address: String): List<CollectibleDto>

    @GET("v1/tokens/{address}/")
    suspend fun loadTokenInfo(@Path("address") address: String): ServiceTokenInfo

    @GET("v1/safes/{address}")
    suspend fun getSafeInfo(@Path("address") address: String): SafeInfoDto

    companion object {
        const val BASE_URL = BuildConfig.TRANSACTION_SERVICE_URL
    }
}
