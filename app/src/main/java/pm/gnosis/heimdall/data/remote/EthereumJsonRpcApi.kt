package pm.gnosis.heimdall.data.remote

import io.reactivex.Observable
import pm.gnosis.heimdall.app.core.BuildConfig
import pm.gnosis.heimdall.data.model.JsonRpcRequest
import pm.gnosis.heimdall.data.model.JsonRpcResult
import retrofit2.http.Body
import retrofit2.http.POST

interface EthereumJsonRpcApi {
    companion object {
        const val BASE_URL: String = BuildConfig.BLOCKCHAIN_NET_URL
    }

    @POST("/")
    fun post(@Body jsonRpcRequest: JsonRpcRequest): Observable<JsonRpcResult>

    @POST("/")
    fun post(@Body jsonRpcRequest: Collection<JsonRpcRequest>): Observable<Collection<JsonRpcResult>>
}
