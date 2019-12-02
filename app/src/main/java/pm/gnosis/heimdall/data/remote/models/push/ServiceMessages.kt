package pm.gnosis.heimdall.data.remote.models.push

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

sealed class ServiceMessage {
    @JsonClass(generateAdapter = true)
    data class SafeCreation(
        @Json(name = "safe") val safe: String,
        @Json(name = "owners") val owners: String,
        @Json(name = "type") val type: String = "safeCreation" // Workaround since moshi is not parsing parent or non-constructor fields
    ) : ServiceMessage()

    @JsonClass(generateAdapter = true)
    data class RequestConfirmation(
        @Json(name = "hash") val hash: String,
        @Json(name = "safe") val safe: String,
        @Json(name = "to") val to: String,
        @Json(name = "value") val value: String,
        @Json(name = "data") val data: String,
        @Json(name = "operation") val operation: String,
        @Json(name = "txGas") val txGas: String,
        @Json(name = "dataGas") val dataGas: String,
        @Json(name = "operationalGas") val operationalGas: String,
        @Json(name = "gasPrice") val gasPrice: String,
        @Json(name = "gasToken") val gasToken: String,
        @Json(name = "refundReceiver") val refundReceiver: String,
        @Json(name = "nonce") val nonce: String,
        @Json(name = "type") val type: String = "requestConfirmation" // Workaround since moshi is not parsing parent or non-constructor fields
    ) : ServiceMessage()

    @JsonClass(generateAdapter = true)
    data class SendTransactionHash(
        @Json(name = "hash") val hash: String,
        @Json(name = "chainHash") val txHash: String,
        @Json(name = "type") val type: String = "sendTransactionHash" // Workaround since moshi is not parsing parent or non-constructor fields
    ) : ServiceMessage()

    @JsonClass(generateAdapter = true)
    data class RejectTransaction(
        @Json(name = "hash") val hash: String,
        @Json(name = "r") val r: String,
        @Json(name = "s") val s: String,
        @Json(name = "v") val v: String,
        @Json(name = "type") val type: String = "rejectTransaction" // Workaround since moshi is not parsing parent or non-constructor fields
    ) : ServiceMessage()

    @JsonClass(generateAdapter = true)
    data class TypedDataConfirmation(
        @Json(name = "hash") val hash: String,
        @Json(name = "signature") val signature: String,
        @Json(name = "type") val type: String = "signTypedDataConfirmation" // Workaround since moshi is not parsing parent or non-constructor fields
    ) : ServiceMessage()
}
