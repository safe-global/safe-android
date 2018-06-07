package pm.gnosis.heimdall.data.remote.models.push

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PushServiceAuth(
    @Json(name = "push_token") val pushToken: String,
    @Json(name = "signature") val signature: ServiceSignature
)
