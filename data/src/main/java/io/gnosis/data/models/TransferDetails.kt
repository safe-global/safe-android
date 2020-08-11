package io.gnosis.data.models

import io.gnosis.data.backend.dto.DetailedExecutionInfo
import io.gnosis.data.backend.dto.TxData
import pm.gnosis.model.Solidity
import java.util.*

interface TransactionDetails

// Designs:
// incoming Transfer https://zpl.io/V1KKZq5 (with data section)
//
data class TransferDetails(
    val txHash: String?,
    val txStatus: TransactionStatus,
    val createdAt: Date?,
    val executedAt: Date?,
    val executor: Solidity.Address,
    val txData: TxData?,
    val detailedExecutionInfo: DetailedExecutionInfo?
) : TransactionDetails

// Designs:
// Contract interaction https://zpl.io/a79BYmp (data collapsed)
// Contract interaction https://zpl.io/aXAJdOE (data expanded)
// Incoming custom transfer https://zpl.io/V1KKZq5
data class CustomDetails(
    val txHash: String?,
    val txStatus: TransactionStatus,
    val createdAt: Date?,
    val executedAt: Date?,
    val detailedExecutionInfo: DetailedExecutionInfo?,
    val txData: TxData?
) : TransactionDetails

// enable module https://zpl.io/brwPG87
// disable module https://zpl.io/V03pzqx
// change Threshold https://zpl.io/bAMW69q (Change required confirmations)
// set fallback handler https://zpl.io/VOO1yPL
// Add owner & change threshold https://zpl.io/aXAJd6E
// Remove owner & change threshold https://zpl.io/VkRoJ45
// Remove owner & Add owner https://zpl.io/2pGXZ8j
// New mastercopy https://zpl.io/2yZYX3n
data class SettingsChangeDetails(
    val txHash: String?,
    val txStatus: TransactionStatus,
    val createdAt: Date?,
    val executedAt: Date?,
    val detailedExecutionInfo: DetailedExecutionInfo?,
    val txData: TxData? // contains data decoded
) : TransactionDetails

// Safe created
data class CreationDetails(
    val txHash: String?,
    val txStatus: TransactionStatus,
    val createdAt: Date?,
    val executedAt: Date?,
    val txData: TxData?,
    val detailedExecutionInfo: DetailedExecutionInfo?

) : TransactionDetails
