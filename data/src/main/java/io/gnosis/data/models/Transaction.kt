package io.gnosis.data.models

import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.data.backend.dto.ServiceTokenInfo
import io.gnosis.data.models.TransactionStatus.*
import pm.gnosis.model.Solidity
import java.math.BigInteger

sealed class Transaction {
    abstract val status: TransactionStatus
    // If status is Successful, Failed or Canceled, the confirmations can be null
    abstract val confirmations: Int?

    data class Custom(
        override val status: TransactionStatus,
        override val confirmations: Int?,
        val nonce: BigInteger?,
        val address: Solidity.Address,
        val dataSize: Long,
        val date: String?,
        val value: BigInteger
    ) : Transaction()

    data class SettingsChange(
        override val status: TransactionStatus,
        override val confirmations: Int?,
        val dataDecoded: DataDecodedDto,
        val date: String?,
        val nonce: BigInteger
    ) : Transaction() {
        fun isChangeMasterCopy(): Boolean {
            return "changeMasterCopy" == dataDecoded.method
        }
    }

    data class Transfer(
        override val status: TransactionStatus,
        override val confirmations: Int?,
        val recipient: Solidity.Address,
        val sender: Solidity.Address,
        val value: BigInteger,
        val date: String?,
        val tokenInfo: ServiceTokenInfo?,
        val nonce: BigInteger?
    ) : Transaction()

    fun isCompleted(): Boolean =
        when (status) {
            AwaitingConfirmation,
            AwaitingExecution,
            Pending -> false
            Success,
            Failed,
            Cancelled -> true
        }
}

enum class TransactionStatus {
    AwaitingConfirmation,
    AwaitingExecution,
    Cancelled,
    Failed,
    Success,
    Pending // Not supported yet as these correspond to the ones issued from the device
}
