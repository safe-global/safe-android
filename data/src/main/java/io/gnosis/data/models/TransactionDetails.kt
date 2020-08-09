package io.gnosis.data.models

import io.gnosis.data.backend.dto.DetailedExecutionInfo
import io.gnosis.data.backend.dto.TxData
import pm.gnosis.model.Solidity
import java.util.*

//FIXME: (WIP) revise structure
data class TransactionDetails(
    val txHash: String,
    val txStatus: TransactionStatus,
    val createdAt: Date?,
    val executedAt: Date?,
    val executor: Solidity.Address,
    val txData: TxData,
    val detailedExecutionInfo: DetailedExecutionInfo
)
