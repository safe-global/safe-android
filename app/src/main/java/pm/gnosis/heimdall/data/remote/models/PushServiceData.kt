package pm.gnosis.heimdall.data.remote.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RequestSignatureData(
    @Json(name = "uri")
    val uri: String
)

@JsonClass(generateAdapter = true)
data class SendSignatureData(
    @Json(name = "uri")
    val uri: String,
    @Json(name = "hash")
    val hash: String
)
