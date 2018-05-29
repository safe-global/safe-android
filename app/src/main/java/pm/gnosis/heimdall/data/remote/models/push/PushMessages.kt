package pm.gnosis.heimdall.data.remote.models.push

import com.squareup.moshi.Json

abstract class PushMessage(@Json(name = "type") val type: String)

data class SafeCreationParams(@Json(name = "safe") val safe: String) : PushMessage("safeCreation")

data class SendTransactionParams(
    @Json(name = "id") val id: String,
    @Json(name = "to") val to: String,
    @Json(name = "value") val value: String,
    @Json(name = "data") val data: String,
    @Json(name = "operation") val operation: Int,
    @Json(name = "signatures") val signatures: List<PushServiceSignature>
) : PushMessage("sendTransaction")

data class SendTransactionHashParams(
    @Json(name = "id") val id: String,
    @Json(name = "txHash") val txHash: String
) : PushMessage("sendTransactionHash")
