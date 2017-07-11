package pm.gnosis.android.app.wallet.data.model

import com.squareup.moshi.Json

data class JsonRpcResult(@Json(name = "id") val id: Int,
                         @Json(name = "jsonrpc") val jsonRpc: String,
                         @Json(name = "result") val result: String)
