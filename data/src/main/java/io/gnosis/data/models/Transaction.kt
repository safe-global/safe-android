package io.gnosis.data.models

import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.data.backend.dto.ServiceTokenInfo
import pm.gnosis.model.Solidity
import java.math.BigInteger
import java.util.*

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
        val date: Date?,
        val value: BigInteger
    ) : Transaction()

    data class SettingsChange(
        override val status: TransactionStatus,
        override val confirmations: Int?,
        val dataDecoded: DataDecodedDto,
        val date: Date?,
        val nonce: BigInteger
    ) : Transaction()

    data class Transfer(
        override val status: TransactionStatus,
        override val confirmations: Int?,
        val recipient: Solidity.Address,
        val sender: Solidity.Address,
        val value: BigInteger,
        val date: Date?,
        val tokenInfo: ServiceTokenInfo?,
        val nonce: BigInteger?,
        val incoming: Boolean
    ) : Transaction()
}

enum class TransactionStatus() {
    AWAITING_CONFIRMATIONS,
    AWAITING_EXECUTION,
    CANCELLED,
    FAILED,
    SUCCESS,
    PENDING // Not supported yet as these correspond to the ones issued from the device
}
