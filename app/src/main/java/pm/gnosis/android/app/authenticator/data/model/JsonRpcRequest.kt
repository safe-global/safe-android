package pm.gnosis.android.app.authenticator.data.model

import com.squareup.moshi.Json

data class JsonRpcRequest(@Json(name = "jsonrpc") val jsonRpc: String = "2.0",
                          @Json(name = "method") val method: String,
                          @Json(name = "params") val params: List<Any> = emptyList(),
                          @Json(name = "id") val id: Int = 1)
