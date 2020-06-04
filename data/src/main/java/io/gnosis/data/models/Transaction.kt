package io.gnosis.data.models

import io.gnosis.data.backend.dto.ServiceTokenInfo
import pm.gnosis.model.Solidity
import java.math.BigInteger

sealed class Transaction {
    data class Custom(val nonce: BigInteger) : Transaction()

    data class SettingsChange(
        val dataDecoded: DataDecodedDto,
        val date: String?,
        val nonce: BigInteger
    ) : Transaction()

    data class Transfer(
        val recipient: Solidity.Address,
        val sender: Solidity.Address,
        val value: BigInteger,
        val date: String?,
        val tokenInfo: ServiceTokenInfo?
    ) : Transaction()
}

data class TransactionDto(
    val safe: Solidity.Address? = null,
    val to: Solidity.Address,
    val value: BigInteger,
    val data: String? = null,
    val dataDecoded: DataDecodedDto? = null,
    val operation: Operation? = null,
    val gasToken: Solidity.Address? = null,
    val safeTxGas: BigInteger,
    val baseGas: BigInteger,
    val gasPrice: BigInteger,
    val refundReceiver: Solidity.Address? = null,
    val nonce: BigInteger? = null,
    val executionDate: String? = null,
    val submissionDate: String? = null,
    val creationDate: String? = null,
    val modified: String? = null,
    val blockNumber: BigInteger? = null,
    val sender: Solidity.Address? = null,
    val tokenAddress: Solidity.Address? = null,
    val tokenInfo: ServiceTokenInfo? = null,
    val transfers: List<TransferDto>? = null,
    val confirmations: List<ConfirmationDto>? = null,
    val txType: TransactionType
)

data class TransferDto(
    val to: Solidity.Address,
    val from: Solidity.Address,
    val type: TransferType,
    val executionDate: String? = null,
    val value: BigInteger,
    val tokenAddress: String? = null, //TokenInfo https://github.com/gnosis/safe-transaction-service/issues/96
    val tokenId: String? = null,
    val transactionHash: String? = null
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

data class DataDecodedDeprecate(
    val json: Pair<String, List<ParamsDto>>
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
