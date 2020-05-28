package io.gnosis.data.models

import io.gnosis.data.backend.dto.ServiceTokenInfo
import pm.gnosis.model.Solidity
import java.math.BigInteger

sealed class Transaction {
    data class Custom(val nonce: BigInteger) : Transaction()

    data class SettingsChange(val nonce: BigInteger) : Transaction()

    data class Transfer(
        val receiver: Solidity.Address,
        val value: BigInteger,
        val executionDate: String?,
        val tokenInfo: ServiceTokenInfo?
    ) : Transaction()
}

data class TransactionDto(
    val safe: Solidity.Address,
    val to: Solidity.Address,
    val value: BigInteger,
    val data: String?,
    val operation: Operation,
    val gasToken: Solidity.Address?,
    val safeTxGas: BigInteger,
    val baseGas: BigInteger,
    val gasPrice: BigInteger,
    val refundReceiver: Solidity.Address?,
    val nonce: BigInteger,
    val executionDate: String?,
    val submissionDate: String?,
    val modified: String?,
    val blockNumber: BigInteger?,
    val sender: Solidity.Address?,
    val tokenAddress: Solidity.Address?,
    val tokenInfo: ServiceTokenInfo?
)

enum class Operation(val id: Int) {
    CALL(0),
    DELEGATE(1)
}
