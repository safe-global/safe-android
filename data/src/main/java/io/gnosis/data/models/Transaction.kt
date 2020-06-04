package io.gnosis.data.models

import io.gnosis.data.backend.dto.ServiceTokenInfo
import pm.gnosis.model.Solidity
import java.math.BigInteger

sealed class Transaction {
    data class Custom(val nonce: BigInteger) : Transaction()

    data class SettingsChange(val nonce: BigInteger) : Transaction()

    data class Transfer(
        val recipient: Solidity.Address,
        val sender: Solidity.Address,
        val value: BigInteger,
        val date: String?,
        val tokenInfo: ServiceTokenInfo?
    ) : Transaction()
}

data class TransactionDto(
    val safe: Solidity.Address,
    val to: Solidity.Address,
    val value: BigInteger,
    val data: String?,
    val dataDecoded: DataDecodedDto?,
    val operation: Operation,
    val gasToken: Solidity.Address?,
    val safeTxGas: BigInteger,
    val baseGas: BigInteger,
    val gasPrice: BigInteger,
    val refundReceiver: Solidity.Address?,
    val nonce: BigInteger?,
    val executionDate: String?,
    val submissionDate: String?,
    val modified: String?,
    val blockNumber: BigInteger?,
    val sender: Solidity.Address?,
    val tokenAddress: Solidity.Address?,
    val tokenInfo: ServiceTokenInfo?,
    val transfers: List<TransferDto>?,
    val confirmations: List<ConfirmationDto>?,
    val txType: TransactionType
)

data class TransferDto(
    val to: Solidity.Address,
    val from: Solidity.Address,
    val type: TransferType,
    val executionDate: String?,
    val value: BigInteger,
    val tokenAddress: String?, //TokenInfo https://github.com/gnosis/safe-transaction-service/issues/96
    val tokenId: String?,
    val transactionHash: String?
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
    val params: List<ParamsDto>
)

data class ParamsDto(
    val type: String,
    val name: String,
    val value: String
)

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
    MODULE_TRANSACTION
}

enum class SignatureType {
    CONTRACT_SIGNATURE,
    APPROVED_HASH,
    EOA,
    ETH_SIGN
}
