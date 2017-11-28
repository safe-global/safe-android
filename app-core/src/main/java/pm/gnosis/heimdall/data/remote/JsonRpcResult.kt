package pm.gnosis.heimdall.data.remote

import com.squareup.moshi.Json
import pm.gnosis.heimdall.data.remote.models.TransactionReceipt

data class JsonRpcResult(@Json(name = "id") val id: Int,
                         @Json(name = "jsonrpc") val jsonRpc: String,
                         @Json(name = "error") val error: JsonRpcError? = null,
                         @Json(name = "result") val result: String)

data class JsonRpcError(@Json(name = "code") val code: Int,
                        @Json(name = "message") val message: String)

data class JsonRpcTransactionReceiptResult(@Json(name = "id") val id: Int,
                         @Json(name = "jsonrpc") val jsonRpc: String,
                         @Json(name = "error") val error: JsonRpcError? = null,
                         @Json(name = "result") val result: TransactionReceipt)
