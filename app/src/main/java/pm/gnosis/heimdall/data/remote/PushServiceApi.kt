package pm.gnosis.heimdall.data.remote

import io.reactivex.Completable
import pm.gnosis.heimdall.data.remote.models.RequestSignatureData
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface PushServiceApi {

    companion object {
        const val BASE_URL = "http://10.0.2.2:8000"
        const val HTTP_SENDER_SIGNATURE = "Sender-Signature"
    }

    // TODO this doesn't work
    @POST("api/1/request_signatures/{safe_address}")
    fun requestSignatures(@Header(HTTP_SENDER_SIGNATURE) signature: String, @Path("safe_address") safeAddress: String, @Body data: RequestSignatureData): Completable
}