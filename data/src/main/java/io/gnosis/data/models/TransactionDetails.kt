package io.gnosis.data.models

import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.data.backend.dto.Operation
import io.gnosis.data.backend.dto.TransactionDirection
import pm.gnosis.model.Solidity
import java.math.BigInteger
import java.util.*

data class DomainTransactionDetails(
    val txHash: String?,
    val txStatus: TransactionStatus,
    val txInfo: DomainTransactionInfo,
    val executedAt: Date?,
    val txData: DomainTxData?,
    val detailedExecutionInfo: DomainDetailedExecutionInfo?
)

data class DomainTxData(
    val hexData: String?,
    val dataDecoded: DataDecodedDto?,
    val to: Solidity.Address,
    val value: BigInteger?,
    val operation: Operation
)

sealed class DomainDetailedExecutionInfo {
    data class DomainMultisigExecutionDetails(
        val submittedAt: Date,
        val nonce: BigInteger,
        val safeTxHash: String,
        val signers: List<Solidity.Address>,
        val confirmationsRequired: Int,
        val confirmations: List<DomainConfirmations>
    ) : DomainDetailedExecutionInfo()

    data class DomainModuleExecutionDetails(
        val address: String
    ) : DomainDetailedExecutionInfo()
}

sealed class DomainTransactionInfo {
    data class Custom(
        val to: Solidity.Address,
        val dataSize: Int,
        val value: String
    ) : DomainTransactionInfo()

    data class SettingsChange(
        val dataDecoded: DataDecodedDto
    ) : DomainTransactionInfo()

    data class Transfer(
        val sender: Solidity.Address,
        val recipient: Solidity.Address,
        val transferInfo: DomainTransferInfo,
        val direction: TransactionDirection
    ) : DomainTransactionInfo()

    object Creation : DomainTransactionInfo()

    object Unknown : DomainTransactionInfo()
}

sealed class DomainTransferInfo {
    data class DomainErc20Transfer(
        val tokenAddress: Solidity.Address,
        val tokenName: String?,
        val tokenSymbol: String?,
        val logoUri: String?,
        val decimals: Int?,
        val value: String
    ) : DomainTransferInfo()

    data class DomainErc721Transfer(
        val tokenAddress: Solidity.Address,
        val tokenId: String,
        val tokenName: String?,
        val tokenSymbol: String?,
        val logoUri: String?
    ) : DomainTransferInfo()

    data class DomainEtherTransfer(
        val value: String
    ) : DomainTransferInfo()
}

data class DomainConfirmations(
    val signer: Solidity.Address,
    val signature: String
)
