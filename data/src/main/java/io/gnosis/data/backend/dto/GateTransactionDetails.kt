package io.gnosis.data.backend.dto

import io.gnosis.data.models.TransactionStatus
import pm.gnosis.model.Solidity
import java.math.BigInteger

//FIXME: (WIP) revise structure
data class GateTransactionDetailsDto(
    val txHash: String?,
    val txStatus: TransactionStatus,
    val txInfo: TxInfo,
    val executedAt: Long?,
    val txData: TxData?,
    val detailedExecutionInfo: DetailedExecutionInfo?
)

//TODO: add missing fields
data class TxInfo(
    val type: String,
    val sender: Solidity.Address
)

data class TxData(
    val hexData: String?,
    val dataDecoded: DataDecodedDto,
    val to: Solidity.Address,
    val value: BigInteger,
    val operation: Operation
)

data class DetailedExecutionInfo(
    val type: String,
    val submittedAt: Long?,
    val nonce: BigInteger,
    val safeTxHash: String,
    val signers: List<Solidity.Address>,
    val confirmationsRequired: Int,
    val confirmations: List<Confirmations>
)

data class Confirmations(
    val signer: Solidity.Address,
    val signature: String
)
