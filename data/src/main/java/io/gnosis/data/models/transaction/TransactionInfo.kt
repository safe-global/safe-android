package io.gnosis.data.models.transaction

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import pm.gnosis.model.Solidity
import java.math.BigInteger

sealed class TransactionInfo(
    @Json(name = "type") val type: TransactionType
) {
    @JsonClass(generateAdapter = true)
    data class Custom(
        @Json(name = "to")
        val to: Solidity.Address,
        @Json(name = "dataSize")
        val dataSize: Int,
        @Json(name = "value")
        val value: BigInteger,
        @Json(name = "methodName")
        val methodName: String?
    ) : TransactionInfo(TransactionType.Custom)

    @JsonClass(generateAdapter = true)
    data class SettingsChange(
        @Json(name = "dataDecoded")
        val dataDecoded: DataDecoded,
        @Json(name = "settingsInfo")
        val settingsInfo: SettingsInfo?
    ) : TransactionInfo(TransactionType.SettingsChange)

    @JsonClass(generateAdapter = true)
    data class Transfer(
        @Json(name = "sender")
        val sender: Solidity.Address,
        @Json(name = "recipient")
        val recipient: Solidity.Address,
        @Json(name = "transferInfo")
        val transferInfo: TransferInfo,
        @Json(name = "direction")
        val direction: TransactionDirection
    ) : TransactionInfo(TransactionType.Transfer)

    @JsonClass(generateAdapter = true)
    data class Creation(
        @Json(name = "creator")
        val creator: Solidity.Address,
        @Json(name = "transactionHash")
        val transactionHash: String,
        @Json(name = "implementation")
        val implementation: Solidity.Address?,
        @Json(name = "factory")
        val factory: Solidity.Address?
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
