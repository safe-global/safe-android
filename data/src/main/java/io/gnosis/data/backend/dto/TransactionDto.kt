package io.gnosis.data.backend.dto

import pm.gnosis.model.Solidity
import java.math.BigInteger

interface TransactionDto {
    val txType: TransactionType?
    val to: Solidity.Address
    val data: String?
}

data class MultisigTransactionDto(
    override val txType: TransactionType = TransactionType.MULTISIG_TRANSACTION,
    override val to: Solidity.Address,
    override val data: String? = null,
    val safe: Solidity.Address,
    val value: BigInteger,
    val contractInfo: ContractInfoDto? = null,
    val dataDecoded: DataDecodedDto? = null,
    val operation: Operation? = null,
    val gasToken: Solidity.Address? = null,
    val safeTxGas: BigInteger,
    val baseGas: BigInteger,
    val gasPrice: BigInteger,
    val refundReceiver: Solidity.Address? = null,
    val nonce: BigInteger,
    val isExecuted: Boolean,
    val isSuccessful: Boolean? = null,
    val executionDate: String? = null,
    val submissionDate: String? = null,
    val creationDate: String? = null,
    val modified: String? = null,
    val blockNumber: BigInteger? = null,
    val transfers: List<TransferDto>? = null,
    val confirmations: List<ConfirmationDto>? = null
) : TransactionDto

data class EthereumTransactionDto(
    override val txType: TransactionType = TransactionType.ETHEREUM_TRANSACTION,
    override val to: Solidity.Address,
    override val data: String? = null,
    val from: Solidity.Address,
    val value: BigInteger?,
    val blockTimestamp: String?,
    val txHash: String,
    val transfers: List<TransferDto>?
) : TransactionDto

object UnknownTransactionDto : TransactionDto {
    override val txType: TransactionType = TransactionType.UNKNOWN
    override val to: Solidity.Address = Solidity.Address(BigInteger.ZERO)
    override val data: String? = null
}

data class ModuleTransactionDto(
    override val txType: TransactionType = TransactionType.MULTISIG_TRANSACTION,
    override val to: Solidity.Address,
    override val data: String? = null,
    val nonce: BigInteger? = null,
    val created: String? = null,
    val blockNumber: BigInteger? = null,
    val transactionHash: String? = null,
    val safe: Solidity.Address,
    val module: Solidity.Address,
    val value: BigInteger? = null,
    val operation: Operation? = null,
    val transfers: List<TransferDto>? = null
) : TransactionDto

data class TransferDto(
    val to: Solidity.Address,
    val from: Solidity.Address,
    val type: TransferType,
    val executionDate: String? = null,
    val value: BigInteger?,
    val tokenAddress: String? = null, // TokenInfo https://github.com/gnosis/safe-transaction-service/issues/96
    val serviceTokenInfo: ServiceTokenInfo? = null,
    val tokenId: String? = null,
    val transactionHash: String? = null
)

data class ContractInfoDto(
    val type: ContractInfoType
)

data class ConfirmationDto(
    val owner: Solidity.Address,
    val submissionDate: String?,
    val transactionHash: String?,
    val signature: String,
    val signatureType: SignatureType
)

data class DataDecodedDto(
    val method: String,
    val parameters: List<ParamsDto>?
)

data class ParamsDto(
    val name: String,
    val type: String,
    val value: String
)

enum class ContractInfoType {
    ERC20, // default
    ERC721
}

enum class Operation(val id: Int) {
    CALL(0),
    DELEGATE(1)
}

enum class TransferType {
    ETHER_TRANSFER,
    ERC20_TRANSFER,
    ERC721_TRANSFER,
    UNKNOWN
}

enum class TransactionType {
    ETHEREUM_TRANSACTION,
    MULTISIG_TRANSACTION,
    MODULE_TRANSACTION,
    UNKNOWN
}

enum class SignatureType {
    CONTRACT_SIGNATURE,
    APPROVED_HASH,
    EOA,
    ETH_SIGN
}
