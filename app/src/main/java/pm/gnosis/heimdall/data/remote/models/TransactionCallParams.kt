package pm.gnosis.heimdall.data.remote.models

import com.squareup.moshi.Json

data class TransactionCallParams(
    @Json(name = "from") val from: String? = null,
    @Json(name = "to") val to: String? = null,
    @Json(name = "gas") val gas: String? = null,
    @Json(name = "gasPrice") val gasPrice: String? = null,
    @Json(name = "value") val value: String? = null,
    @Json(name = "data") val data: String? = null,
    @Json(name = "nonce") val nonce: String? = null
) {
    fun callRequest(id: Int, block: String = "latest"): JsonRpcRequest {
        return JsonRpcRequest(
            id = id, method = "eth_call",
            params = arrayListOf(this, block)
        )
    }
}
