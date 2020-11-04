package io.gnosis.data.models

import io.gnosis.data.backend.dto.DataDecodedDto
import pm.gnosis.model.Solidity
import java.math.BigInteger
import java.util.*

sealed class Transaction {
    abstract val id: String
    abstract val status: TransactionStatus

    // If status is Successful, Failed or Canceled, the confirmations can be null
    abstract val confirmations: Int?
    abstract val missingSigners: List<Solidity.Address>?

    data class Custom(
        override val id: String,
        override val status: TransactionStatus,
        override val confirmations: Int?,
        override val missingSigners: List<Solidity.Address>?,
        val nonce: BigInteger?,
        val address: Solidity.Address,
        val dataSize: Int,
        val date: Date?,
        val value: BigInteger
    ) : Transaction()

    data class SettingsChange(
        override val id: String,
        override val status: TransactionStatus,
        override val confirmations: Int?,
        override val missingSigners: List<Solidity.Address>?,
        val dataDecoded: DataDecodedDto,
        val date: Date?,
        val nonce: BigInteger
    ) : Transaction()

    data class Transfer(
        override val id: String,
        override val status: TransactionStatus,
        override val confirmations: Int?,
        override val missingSigners: List<Solidity.Address>?,
        val recipient: Solidity.Address,
        val sender: Solidity.Address,
        val value: BigInteger,
        val date: Date?,
        val tokenInfo: TokenInfo?,
        val nonce: BigInteger?,
        val incoming: Boolean
    ) : Transaction()

    data class Creation(
        override val id: String,
        override val status: TransactionStatus = TransactionStatus.SUCCESS,
        override val confirmations: Int? = null,
        override val missingSigners: List<Solidity.Address>? = null,
        val timestamp: Date,
        val txInfo: TransactionInfo,
        val executionInfo: DetailedExecutionInfo?
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
