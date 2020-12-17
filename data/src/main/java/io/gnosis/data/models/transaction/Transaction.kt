package io.gnosis.data.models.transaction

import com.squareup.moshi.Json
import pm.gnosis.model.Solidity
import java.math.BigInteger
import java.util.*

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
    val executionInfo: ExecutionInfo?
)

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

enum class TransactionType {
    Transfer,
    SettingsChange,
    Custom,
    Creation,
    Unknown
}

enum class TransactionStatus {
    AWAITING_CONFIRMATIONS,
    AWAITING_EXECUTION,
    CANCELLED,
    FAILED,
    SUCCESS,
    PENDING // Not supported yet as these correspond to the ones issued from the device
}
