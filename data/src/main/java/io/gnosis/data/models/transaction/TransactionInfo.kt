package io.gnosis.data.models.transaction

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.AddressInfoExtended
import pm.gnosis.model.Solidity
import java.math.BigInteger

sealed class TransactionInfo(
    @Json(name = "type") val type: TransactionType
) {
    @JsonClass(generateAdapter = true)
    data class Custom(
        @Json(name = "to") val to: AddressInfoExtended,
        @Json(name = "toInfo") val toInfo: AddressInfo? = null,
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
        @Json(name = "sender") val sender: AddressInfoExtended,
        @Json(name = "senderInfo") val senderInfo: AddressInfo?,
        @Json(name = "recipient") val recipient: AddressInfoExtended,
        @Json(name = "recipientInfo") val recipientInfo: AddressInfo?,
        @Json(name = "transferInfo") val transferInfo: TransferInfo,
        @Json(name = "direction") val direction: TransactionDirection
    ) : TransactionInfo(TransactionType.Transfer)

    @JsonClass(generateAdapter = true)
    data class Creation(
        @Json(name = "creator") val creator: Solidity.Address,
        @Json(name = "creatorInfo") val creatorInfo: AddressInfo?,
        @Json(name = "transactionHash") val transactionHash: String,
        @Json(name = "implementation") val implementation: Solidity.Address?,
        @Json(name = "implementationInfo") val implementationInfo: AddressInfo?,
        @Json(name = "factory") val factory: Solidity.Address?,
        @Json(name = "factoryInfo") val factoryInfo: AddressInfo?
    ) : TransactionInfo(TransactionType.Creation)

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
