package io.gnosis.data.models.transaction

import io.gnosis.data.backend.dto.DataDecodedDto
import pm.gnosis.model.Solidity
import java.math.BigInteger

sealed class TransactionInfo(val type: TransactionType) {

    data class Custom(
        val to: Solidity.Address,
        val dataSize: Int,
        val value: BigInteger
    ) : TransactionInfo(TransactionType.Custom)

    data class SettingsChange(
        val dataDecoded: DataDecodedDto
    ) : TransactionInfo(TransactionType.SettingsChange)

    data class Transfer(
        val sender: Solidity.Address,
        val recipient: Solidity.Address,
        val transferInfo: TransferInfo,
        val direction: TransactionDirection
    ) : TransactionInfo(TransactionType.Transfer)

    data class Creation(
        val creator: Solidity.Address,
        val transactionHash: String,
        val implementation: Solidity.Address?,
        val factory: Solidity.Address?
    ) : TransactionInfo(TransactionType.Creation)

    object Unknown : TransactionInfo(TransactionType.Unknown)
}

enum class TransactionDirection {
    INCOMING,
    OUTGOING,
    UNKNOWN
}
