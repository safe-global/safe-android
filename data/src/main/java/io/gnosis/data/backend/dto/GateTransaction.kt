package io.gnosis.data.backend.dto

import io.gnosis.data.models.TransactionStatus
import pm.gnosis.model.Solidity
import java.math.BigInteger

data class GateTransactionDto(
    val id: String,
    val timestamp: Long,
    val txStatus: TransactionStatus,
    val txInfo: TransactionInfoDto,
    val executionInfo: ExecutionInfoDto?
)

data class ExecutionInfoDto(
    val nonce: BigInteger,
    val confirmationsRequired: Int,
    val confirmationsSubmitted: Int
)

enum class GateTransactionType {
    Transfer,
    SettingsChange,
    Custom,
    Creation,
    Unknown
}

sealed class TransactionInfoDto {
    abstract val type: GateTransactionType

    data class Custom(
        override val type: GateTransactionType = GateTransactionType.Custom,
        val to: Solidity.Address,
        val dataSize: Int,
        val value: BigInteger
    ) : TransactionInfoDto()

    data class SettingsChange(
        override val type: GateTransactionType = GateTransactionType.SettingsChange,
        val dataDecoded: DataDecodedDto
    ) : TransactionInfoDto()

    data class Transfer(
        override val type: GateTransactionType = GateTransactionType.Transfer,
        val sender: Solidity.Address,
        val recipient: Solidity.Address,
        val transferInfo: TransferInfoDto,
        val direction: TransactionDirection
    ) : TransactionInfoDto()

    data class Creation(
        override val type: GateTransactionType = GateTransactionType.Creation,
        val creator: Solidity.Address,
        val transactionHash: String,
        val implementation: Solidity.Address?,
        val factory: Solidity.Address?
    ) : TransactionInfoDto()

    data class Unknown(
        override val type: GateTransactionType = GateTransactionType.Unknown
    ) : TransactionInfoDto()
}

enum class TransactionDirection {
    INCOMING,
    OUTGOING,
    UNKNOWN
}

enum class GateTransferType {
    ERC20, ERC721, ETHER
}

sealed class TransferInfoDto {
    abstract val type: GateTransferType

    data class Erc20Transfer(
        override val type: GateTransferType = GateTransferType.ERC20,
        val tokenAddress: Solidity.Address,
        val tokenName: String?,
        val tokenSymbol: String?,
        val logoUri: String?,
        val decimals: Int?,
        val value: BigInteger
    ) : TransferInfoDto()

    data class Erc721Transfer(
        override val type: GateTransferType = GateTransferType.ERC721,
        val tokenAddress: Solidity.Address,
        val tokenId: String,
        val tokenName: String?,
        val tokenSymbol: String?,
        val logoUri: String?
    ) : TransferInfoDto()

    data class EtherTransfer(
        override val type: GateTransferType = GateTransferType.ETHER,
        val value: BigInteger
    ) : TransferInfoDto()
}
