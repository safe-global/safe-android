package pm.gnosis.android.app.authenticator.data.remote

import io.reactivex.Observable
import pm.gnosis.android.app.authenticator.data.model.JsonRpcRequest
import pm.gnosis.android.app.authenticator.data.model.JsonRpcResult
import retrofit2.http.Body
import retrofit2.http.POST

interface InfuraApi {
    companion object {
        const val MAINNET_NETWORK = "mainnet"
        const val ROPSTEN_NETWORK = "ropsten"
        const val KOVAN_NETWORK = "kovan"
        const val RINKEBY_NETWORK = "rinkeby"

        const val RINKEBY_BASE_URL = "https://mainnet.infura.io/"
    }

    @POST("/")
    fun post(@Body jsonRpcRequest: JsonRpcRequest): Observable<JsonRpcResult>
}
