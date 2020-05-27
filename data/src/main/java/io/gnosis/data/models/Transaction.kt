package io.gnosis.data.models

import pm.gnosis.model.Solidity
import java.math.BigInteger

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
    val executionDate: String,
    val submissionDate: String,
    val modified: String, //Date
    val blockNumber: BigInteger
)

enum class Operation(val id: Int) {
    CALL(0),
    DELEGATE(1)
}
