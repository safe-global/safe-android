package io.gnosis.data.models.transaction

import pm.gnosis.model.Solidity
import java.math.BigInteger
import java.util.*

data class Transaction(
    val id: String,
    val timestamp: Date,
    val txStatus: TransactionStatus,
    val txInfo: TransactionInfo,
    val executionInfo: ExecutionInfo?
)

data class ExecutionInfo(
    val nonce: BigInteger,
    val confirmationsRequired: Int,
    val confirmationsSubmitted: Int,
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
