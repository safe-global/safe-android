package io.gnosis.data.backend.dto

import io.gnosis.data.models.TransactionStatus
import pm.gnosis.model.Solidity
import java.math.BigInteger

data class GateTransactionDetailsDto(
    val txHash: String?,
    val txStatus: TransactionStatus,
    val txInfo: TransactionInfoDto,
    val executedAt: Long?,
    val txData: TxDataDto?,
    val detailedExecutionInfo: DetailedExecutionInfoDto?
)

data class TxDataDto(
    val hexData: String?,
    val dataDecoded: DataDecodedDto?,
    val to: Solidity.Address,
    val value: BigInteger?,
    val operation: Operation
)

sealed class DetailedExecutionInfoDto {
    abstract val type: DetailedExecutionInfoType

    data class MultisigExecutionDetailsDto(
        override val type: DetailedExecutionInfoType,
        val submittedAt: Long,
        val nonce: BigInteger,
        val safeTxHash: String,
        val signers: List<Solidity.Address>,
        val confirmationsRequired: Int,
        val confirmations: List<ConfirmationsDto>,
        val executor: Solidity.Address?
    ) : DetailedExecutionInfoDto()

    data class ModuleExecutionDetailsDto(
        override val type: DetailedExecutionInfoType,
        val address: String
    ) : DetailedExecutionInfoDto()
}

enum class DetailedExecutionInfoType {
    MULTISIG,
    MODULE
}

data class ConfirmationsDto(
    val signer: Solidity.Address,
    val signature: String
)
