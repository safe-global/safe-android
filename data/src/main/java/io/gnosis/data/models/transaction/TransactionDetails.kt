package io.gnosis.data.models.transaction

import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.data.backend.dto.Operation
import pm.gnosis.model.Solidity
import java.math.BigInteger
import java.util.*

data class TransactionDetails(
    val txHash: String?,
    val txStatus: TransactionStatus,
    val txInfo: TransactionInfo,
    val executedAt: Date?,
    val txData: TxData?,
    val detailedExecutionInfo: DetailedExecutionInfo?
)

data class TxData(
    val hexData: String?,
    val dataDecoded: DataDecodedDto?,
    val to: Solidity.Address,
    val value: BigInteger?,
    val operation: Operation
)

enum class DetailedExecutionInfoType {
    MULTISIG,
    MODULE
}

sealed class DetailedExecutionInfo(val type: DetailedExecutionInfoType) {
    data class MultisigExecutionDetails(
        val submittedAt: Date,
        val nonce: BigInteger,
        val safeTxHash: String,
        val signers: List<Solidity.Address>,
        val confirmationsRequired: Int,
        val confirmations: List<Confirmations>,
        val executor: Solidity.Address?,
        val safeTxGas: BigInteger,
        val baseGas: BigInteger,
        val gasPrice: BigInteger,
        val gasToken: Solidity.Address
    ) : DetailedExecutionInfo(DetailedExecutionInfoType.MULTISIG)

    data class ModuleExecutionDetails(
        val address: String
    ) : DetailedExecutionInfo(DetailedExecutionInfoType.MODULE)
}

data class Confirmations(
    val signer: Solidity.Address,
    val signature: String,
    val submittedAt: Date
)
