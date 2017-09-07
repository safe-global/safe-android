package pm.gnosis.android.app.authenticator.data.remote

import io.reactivex.Observable
import pm.gnosis.android.app.core.BuildConfig
import pm.gnosis.android.app.authenticator.data.model.JsonRpcRequest
import pm.gnosis.android.app.authenticator.data.model.JsonRpcResult
import retrofit2.http.Body
import retrofit2.http.POST

interface EthereumJsonRpcApi {
    companion object {
        val BASE_URL: String = BuildConfig.BLOCKCHAIN_NET_URL
    }

    @POST("/")
    fun post(@Body jsonRpcRequest: JsonRpcRequest): Observable<JsonRpcResult>
}
