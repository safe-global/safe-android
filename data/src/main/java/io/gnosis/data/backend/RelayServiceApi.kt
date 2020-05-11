package io.gnosis.data.backend

import io.gnosis.data.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Path
import io.gnosis.data.backend.dto.ServiceTokenInfo

@Deprecated("Use transaction service API if available")
interface RelayServiceApi {

    @GET("v1/tokens/{address}/")
    suspend fun tokenInfo(@Path("address") address: String): ServiceTokenInfo

    companion object {
        const val BASE_URL = BuildConfig.RELAY_SERVICE_URL
    }
}

