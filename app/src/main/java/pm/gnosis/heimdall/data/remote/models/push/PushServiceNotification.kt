package pm.gnosis.heimdall.data.remote.models.push

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PushServiceNotification(
    @Json(name = "devices") val devices: List<String>,
    @Json(name = "message") val message: String,
    @Json(name = "signature") val signature: ServiceSignature
)
