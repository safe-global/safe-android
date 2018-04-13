package pm.gnosis.heimdall.data.remote.models.push

import com.squareup.moshi.Json

data class PushMessage<out T>(
    @Json(name = "type") val type: String,
    @Json(name = "params") val params: T
)

interface PushParams<out T> {
    fun buildMessage(): PushMessage<T>
}

data class SafeCreationParams(@Json(name = "safe") val safe: String) : PushParams<SafeCreationParams> {
    override fun buildMessage(): PushMessage<SafeCreationParams> = PushMessage("safeCreation", this)
}

data class SendTransactionParams(
    @Json(name = "id") val id: String,
    @Json(name = "to") val to: String,
    @Json(name = "value") val value: String,
    @Json(name = "data") val data: String,
    @Json(name = "operation") val operation: Int,
    @Json(name = "signatures") val signatures: List<PushServiceSignature>
) : PushParams<SendTransactionParams> {
    override fun buildMessage(): PushMessage<SendTransactionParams> = PushMessage("sendTransaction", this)
}

data class SendTransactionHashParams(
    @Json(name = "id") val id: String,
    @Json(name = "txHash") val txHash: String
) : PushParams<SendTransactionHashParams> {
    override fun buildMessage(): PushMessage<SendTransactionHashParams> = PushMessage("sendTransactionHash", this)
}
