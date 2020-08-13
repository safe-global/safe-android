package io.gnosis.data.models

import io.gnosis.data.backend.dto.DetailedExecutionInfo
import io.gnosis.data.backend.dto.TxData
import pm.gnosis.model.Solidity
import java.util.*

interface TransactionDetails

data class TransferDetails(
    val txHash: String?,
    val txStatus: TransactionStatus,
    val createdAt: Date?,
    val executedAt: Date?,
    val executor: Solidity.Address,
    val txData: TxData?,
    val detailedExecutionInfo: DetailedExecutionInfo?,
    val incoming: Boolean?
) : TransactionDetails

data class CustomDetails(
    val txHash: String?,
    val txStatus: TransactionStatus,
    val createdAt: Date?,
    val executedAt: Date?,
    val executor: Solidity.Address,
    val detailedExecutionInfo: DetailedExecutionInfo?,
    val txData: TxData?,
    val dataSize: Int
) : TransactionDetails


data class SettingsChangeDetails(
    val txHash: String?,
    val txStatus: TransactionStatus,
    val createdAt: Date?,
    val executedAt: Date?,
    val executor: Solidity.Address,
    val detailedExecutionInfo: DetailedExecutionInfo?,
    val txData: TxData? // contains data decoded
) : TransactionDetails

@Deprecated("Will not be returned from client gateway")
data class CreationDetails(
    val txHash: String?,
    val txStatus: TransactionStatus,
    val createdAt: Date?,
    val executedAt: Date?,
    val executor: Solidity.Address,
    val txData: TxData?,
    val detailedExecutionInfo: DetailedExecutionInfo?

) : TransactionDetails
