package pm.gnosis.heimdall.data.remote.models.push

import com.squareup.moshi.Json

sealed class ServiceMessage(
    @Json(name = "type") val type: String
) {
    data class SafeCreation(
        @Json(name = "safe") val safe: String
    ): ServiceMessage("safeCreation")

    data class RequestConfirmation(
        @Json(name = "hash") val hash: String,
        @Json(name = "safe") val safe: String,
        @Json(name = "to") val to: String,
        @Json(name = "value") val value: String,
        @Json(name = "data") val data: String,
        @Json(name = "operation") val operation: String,
        @Json(name = "txGas") val txGas: String,
        @Json(name = "dataGas") val dataGas: String,
        @Json(name = "gasPrice") val gasPrice: String,
        @Json(name = "gasToken") val gasToken: String,
        @Json(name = "nonce") val nonce: String
    ): ServiceMessage("requestConfirmation")

    data class SendTransactionHash(
        @Json(name = "hash") val hash: String,
        @Json(name = "txHash") val txHash: String
    ) : ServiceMessage("sendTransactionHash")
}
