package io.gnosis.data.models

import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.data.backend.dto.ServiceTokenInfo
import io.gnosis.data.models.TransactionStatus.AwaitingConfirmations
import io.gnosis.data.models.TransactionStatus.AwaitingExecution
import io.gnosis.data.models.TransactionStatus.Cancelled
import io.gnosis.data.models.TransactionStatus.Failed
import io.gnosis.data.models.TransactionStatus.Pending
import io.gnosis.data.models.TransactionStatus.Success
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

        fun isSetFallBackHandler(): Boolean {
            return "setFallbackHandler" == dataDecoded.method
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
            AwaitingConfirmations,
            AwaitingExecution,
            Pending -> false
            Success,
            Failed,
            Cancelled -> true
        }
}

enum class TransactionStatus(val displayString: String) {
    AwaitingConfirmations("Awaiting confirmations"),
    AwaitingExecution("Awaiting execution"),
    Cancelled("Cancelled"),
    Failed("Failed"),
    Success("Success"),
    Pending("Pending") // Not supported yet as these correspond to the ones issued from the device
}
