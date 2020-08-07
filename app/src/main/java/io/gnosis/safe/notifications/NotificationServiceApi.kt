package io.gnosis.safe.notifications

import io.gnosis.safe.notifications.models.FirebaseDevice
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificationServiceApi {

    @POST("/notifications/devices/")
    suspend fun register(@Body data: FirebaseDevice)

    @DELETE("/notifications/devices/{uuid}")
    suspend fun unregister(@Path("uuid") deviceUuid: String)

    @DELETE("/notifications/devices/{uuid}/safes/{address}/")
    suspend fun unregisterSafe(@Path("uuid") deviceUuid: String, @Path("address") safe: String)

    companion object {
        const val BASE_URL = "https://safe-transaction.dev.gnosisdev.com/"
        //const val BASE_URL = BuildConfig.NOTIFICATION_SERVICE_URL
    }
}
