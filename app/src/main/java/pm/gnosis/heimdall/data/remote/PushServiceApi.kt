package pm.gnosis.heimdall.data.remote

import io.reactivex.Completable
import pm.gnosis.heimdall.data.remote.models.push.PushServiceAuth
import pm.gnosis.heimdall.data.remote.models.push.PushServiceNotification
import pm.gnosis.heimdall.data.remote.models.push.PushServicePairing
import retrofit2.http.Body
import retrofit2.http.POST

interface PushServiceApi {
    companion object {
        const val BASE_URL = "https://safe-notification.dev.gnosisdev.com/api/"
    }

    @POST("v1/auth/")
    fun auth(@Body pushServiceAuth: PushServiceAuth): Completable

    @POST("v1/pairing/")
    fun pair(@Body pushServicePairing: PushServicePairing): Completable

    @POST("v1/notifications/")
    fun notify(@Body pushServiceNotification: PushServiceNotification): Completable
}
