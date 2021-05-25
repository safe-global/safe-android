package io.gnosis.data.models.transaction

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.gnosis.data.utils.ExcludeClassFromJacocoGeneratedReport
import pm.gnosis.model.Solidity
import java.math.BigInteger
import java.util.*

@ExcludeClassFromJacocoGeneratedReport
@JsonClass(generateAdapter = true)
data class Transaction(
    @Json(name = "id")
    val id: String,
    @Json(name = "timestamp")
    val timestamp: Date,
    @Json(name = "txStatus")
    val txStatus: TransactionStatus,
    @Json(name = "txInfo")
    val txInfo: TransactionInfo,
    @Json(name = "executionInfo")
    val executionInfo: ExecutionInfo?,
    @Json(name = "safeAppInfo")
    val safeAppInfo: SafeAppInfo?
)

@ExcludeClassFromJacocoGeneratedReport
@JsonClass(generateAdapter = true)
data class SafeAppInfo(
    @Json(name = "name")
    val name: String,
    @Json(name = "url")
    val url: String,
    @Json(name = "logoUrl")
    val logoUrl: String
)

@ExcludeClassFromJacocoGeneratedReport
@JsonClass(generateAdapter = true)
data class ExecutionInfo(
    @Json(name = "nonce")
    val nonce: BigInteger,
    @Json(name = "confirmationsRequired")
    val confirmationsRequired: Int,
    @Json(name = "confirmationsSubmitted")
    val confirmationsSubmitted: Int,
    @Json(name = "missingSigners")
    val missingSigners: List<Solidity.Address>?
)

@ExcludeClassFromJacocoGeneratedReport
enum class TransactionType {
    @Json(name = "Transfer")
    Transfer,

    @Json(name = "SettingsChange")
    SettingsChange,

    @Json(name = "Custom")
    Custom,

    @Json(name = "Creation")
    Creation,

    @Json(name = "Unknown")
    Unknown
}

@ExcludeClassFromJacocoGeneratedReport
enum class TransactionStatus {
    @Json(name = "AWAITING_CONFIRMATIONS")
    AWAITING_CONFIRMATIONS,

    @Json(name = "AWAITING_EXECUTION")
    AWAITING_EXECUTION,

    @Json(name = "CANCELLED")
    CANCELLED,

    @Json(name = "FAILED")
    FAILED,

    @Json(name = "SUCCESS")
    SUCCESS,

    @Json(name = "PENDING")
    PENDING // Not supported yet as these correspond to the ones issued from the device
}
