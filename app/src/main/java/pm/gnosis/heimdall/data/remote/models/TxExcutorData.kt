package pm.gnosis.heimdall.data.remote.models

import com.squareup.moshi.Json

data class TxExecutionData(
    @Json(name = "target")
    val target: String,
    @Json(name = "data")
    val data: String?
)

data class TxExecutionResponse(
    @Json(name = "hash")
    val hash: String
)
