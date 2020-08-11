package io.gnosis.data.backend.dto

import io.gnosis.data.models.TransactionStatus
import pm.gnosis.model.Solidity
import java.math.BigInteger

data class GateTransactionDetailsDto(
    val txHash: String?,
    val txStatus: TransactionStatus,
    val txInfo: TransactionInfo,
    val executedAt: Long?,
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

interface DetailedExecutionInfo {
    val type: DetailedExecutionInfoType
}

data class MultisigExecutionDetails(
    override val type: DetailedExecutionInfoType,
    val submittedAt: Long?,
    val nonce: BigInteger,
    val safeTxHash: String,
    val signers: List<Solidity.Address>,
    val confirmationsRequired: Int,
    val confirmations: List<Confirmations>
) : DetailedExecutionInfo

data class ModuleExecutionDetails(
    override val type: DetailedExecutionInfoType,
    val address: String
) : DetailedExecutionInfo

enum class DetailedExecutionInfoType {
    MULTISIG,
    MODULE
}

data class Confirmations(
    val signer: Solidity.Address,
    val signature: String
)
