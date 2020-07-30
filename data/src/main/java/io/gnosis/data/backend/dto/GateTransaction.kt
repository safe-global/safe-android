package io.gnosis.data.backend.dto

import io.gnosis.data.models.TransactionStatus
import pm.gnosis.model.Solidity

data class GateTransactionDto(
    val id: String,
    val timestamp: Long,
    val txStatus: TransactionStatus,
    val txInfo: TransactionInfo,
    val executionInfo: ExecutionInfo?
)

data class ExecutionInfo(
    val nonce: Long,
    val confirmationsRequired: Int,
    val confirmationsSubmitted: Int
)

enum class GateTransactionType {
    Transfer,
    SettingsChange,
    Custom
}

interface TransactionInfo {
    val type: GateTransactionType
}

data class Custom(
    override val type: GateTransactionType = GateTransactionType.Custom,
    val to: Solidity.Address,
    val dataSize: String,
    val value: String
) : TransactionInfo

data class SettingsChange(
    override val type: GateTransactionType = GateTransactionType.SettingsChange,
    val dataDecoded: DataDecodedDto
) : TransactionInfo

data class Transfer(
    override val type: GateTransactionType = GateTransactionType.Transfer,
    val sender: Solidity.Address,
    val recipient: Solidity.Address,
    val transferInfo: TransferInfo
) : TransactionInfo


enum class GateTransferType {
    ERC20, ERC721, ETHER
}

interface TransferInfo {
    val type: GateTransferType
}

data class Erc20Transfer(
    override val type: GateTransferType = GateTransferType.ERC20,
    val token_address: Solidity.Address,
    val token_name: String?,
    val token_symbol: String?,
    val logo_uri: String?,
    val decimals: Int?,
    val value: String
) : TransferInfo

data class Erc721Transfer(
    override val type: GateTransferType = GateTransferType.ERC721,
    val token_address: Solidity.Address,
    val token_id: String,
    val token_name: String?,
    val token_symbol: String?,
    val logo_uri: String?
) : TransferInfo

data class EtherTransfer(
    override val type: GateTransferType = GateTransferType.ETHER,
    val value: String
) : TransferInfo


