package pm.gnosis.heimdall.data.remote.models.push

import com.squareup.moshi.Json

data class PushServicePairing(
    @Json(name = "signature") val signature: ServiceSignature,
    @Json(name = "temporaryAuthorization") val temporaryAuthorization: PushServiceTemporaryAuthorization
)

data class PushServiceTemporaryAuthorization(
    @Json(name = "signature") val signature: ServiceSignature,
    @Json(name = "expirationDate") val expirationDate: String
)
