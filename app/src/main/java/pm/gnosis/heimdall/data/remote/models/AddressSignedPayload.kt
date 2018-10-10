package pm.gnosis.heimdall.data.remote.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import pm.gnosis.heimdall.data.remote.models.push.ServiceSignature
import pm.gnosis.model.Solidity

@JsonClass(generateAdapter = true)
data class AddressSignedPayload(
    @Json(name = "address") val address: Solidity.Address,
    @Json(name = "signature") val signature: ServiceSignature
)
