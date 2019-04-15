package pm.gnosis.heimdall.data.remote.models.push

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PushServiceAuth(
    @Json(name = "push_token") val pushToken: String,
    @Json(name = "build_number") val buildNumber: Int,
    @Json(name = "version_name") val versionName: String,
    @Json(name = "client") val client: String,
    @Json(name = "bundle") val bundle: String,
    @Json(name = "signatures") val signatures: List<ServiceSignature>
)
