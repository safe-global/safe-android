package io.gnosis.data.models.transaction

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.gnosis.data.models.AddressInfo
import java.math.BigInteger

sealed class TransactionInfo(
    @Json(name = "type") val type: TransactionType
) {
    @JsonClass(generateAdapter = true)
    data class Custom(
        @Json(name = "to") val to: AddressInfo,
        @Json(name = "dataSize") val dataSize: Int = 0,
        @Json(name = "value") val value: BigInteger = BigInteger.ZERO,
        @Json(name = "methodName") val methodName: String? = null,
        @Json(name = "actionCount") val actionCount: Int? = 0,
        @Json(name = "isCancellation") val isCancellation: Boolean = false
    ) : TransactionInfo(TransactionType.Custom)

    @JsonClass(generateAdapter = true)
    data class SettingsChange(
        @Json(name = "dataDecoded") val dataDecoded: DataDecoded,
        @Json(name = "settingsInfo") val settingsInfo: SettingsInfo?
    ) : TransactionInfo(TransactionType.SettingsChange)

    @JsonClass(generateAdapter = true)
    data class Transfer(
        @Json(name = "sender") val sender: AddressInfo,
        @Json(name = "recipient") val recipient: AddressInfo,
        @Json(name = "transferInfo") val transferInfo: TransferInfo,
        @Json(name = "direction") val direction: TransactionDirection
    ) : TransactionInfo(TransactionType.Transfer)

    @JsonClass(generateAdapter = true)
    data class Creation(
        @Json(name = "creator") val creator: AddressInfo,
        @Json(name = "transactionHash") val transactionHash: String,
        @Json(name = "implementation") val implementation: AddressInfo?,
        @Json(name = "factory") val factory: AddressInfo?
    ) : TransactionInfo(TransactionType.Creation)

    @JsonClass(generateAdapter = true)
    data class SwapOrder(
        @Json(name = "uid") val uid: String,
        @Json(name = "explorerUrl") val explorerUrl: String
    ) : TransactionInfo(TransactionType.SwapOrder)

    @JsonClass(generateAdapter = true)
    data class SwapTransfer(
        @Json(name = "uid") val uid: String,
        @Json(name = "explorerUrl") val explorerUrl: String
    ) : TransactionInfo(TransactionType.SwapTransfer)

    @JsonClass(generateAdapter = true)
    data class TwapOrder(
        @Json(name = "status") val status: String,
        @Json(name = "kind") val kind: String,
    ) : TransactionInfo(TransactionType.TwapOrder)

    object Unknown : TransactionInfo(TransactionType.Unknown)
}

enum class TransactionDirection {
    @Json(name = "INCOMING")
    INCOMING,

    @Json(name = "OUTGOING")
    OUTGOING,

    @Json(name = "UNKNOWN")
    UNKNOWN
}
