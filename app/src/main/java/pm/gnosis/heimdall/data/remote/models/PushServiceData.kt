package pm.gnosis.heimdall.data.remote.models

import com.squareup.moshi.Json

data class RequestSignatureData(
    @Json(name = "uri")
    val uri: String
)

data class SendSignatureData(
    @Json(name = "uri")
    val uri: String,
    @Json(name = "hash")
    val hash: String
)
