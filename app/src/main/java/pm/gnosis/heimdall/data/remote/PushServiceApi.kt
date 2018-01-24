package pm.gnosis.heimdall.data.remote

import io.reactivex.Completable
import pm.gnosis.heimdall.data.remote.models.RequestSignatureData
import pm.gnosis.heimdall.data.remote.models.SendSignatureData
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface PushServiceApi {

    companion object {
        const val BASE_URL = "https://gnosis-safe-push.herokuapp.com"
        const val HTTP_SENDER_SIGNATURE = "Sender-Signature"
    }

    @POST("api/1/request_signatures/{safe_address}")
    fun requestSignatures(@Header(HTTP_SENDER_SIGNATURE) signature: String, @Path("safe_address") safeAddress: String, @Body data: RequestSignatureData): Completable

    @POST("api/1/send_signature/{safe_address}")
    fun sendSignature(@Path("safe_address") safeAddress: String, @Body data: SendSignatureData): Completable
}
