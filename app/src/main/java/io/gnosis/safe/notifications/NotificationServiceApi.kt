package io.gnosis.safe.notifications

import io.gnosis.data.BuildConfig
import io.gnosis.safe.notifications.models.Registration
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificationServiceApi {

    @POST("/api/v1/notifications/devices/")
    suspend fun register(@Body data: Registration): Registration

    @DELETE("/api/v1/notifications/devices/{uuid}")
    suspend fun unregister(@Path("uuid") deviceUuid: String)

    @DELETE("/api/v1/notifications/devices/{uuid}/safes/{address}/")
    suspend fun unregisterSafe(@Path("uuid") deviceUuid: String, @Path("address") safe: String)

    companion object {
        //const val BASE_URL = "https://safe-transaction.dev.gnosisdev.com/"
        const val BASE_URL = BuildConfig.TRANSACTION_SERVICE_URL
    }
}
