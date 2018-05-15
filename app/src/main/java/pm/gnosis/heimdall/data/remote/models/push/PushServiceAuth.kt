package pm.gnosis.heimdall.data.remote.models.push

import com.squareup.moshi.Json

data class PushServiceAuth(
    @Json(name = "push_token") val pushToken: String,
    @Json(name = "signature") val signature: PushServiceSignature
)
