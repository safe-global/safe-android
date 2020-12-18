package io.gnosis.data.models.transaction

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import pm.gnosis.model.Solidity
import java.math.BigInteger
import java.util.*

@JsonClass(generateAdapter = true)
data class TransactionDetails(
    @Json(name = "txHash")
    val txHash: String?,
    @Json(name = "txStatus")
    val txStatus: TransactionStatus,
    @Json(name = "txInfo")
    val txInfo: TransactionInfo,
    @Json(name = "executedAt")
    val executedAt: Date?,
    @Json(name = "txData")
    val txData: TxData?,
    @Json(name = "detailedExecutionInfo")
    val detailedExecutionInfo: DetailedExecutionInfo?
)

@JsonClass(generateAdapter = true)
data class TxData(
    @Json(name = "hexData")
    val hexData: String?,
    @Json(name = "dataDecoded")
    val dataDecoded: DataDecoded?,
    @Json(name = "to")
    val to: Solidity.Address,
    @Json(name = "value")
    val value: BigInteger?,
    @Json(name = "operation")
    val operation: Operation
)

enum class DetailedExecutionInfoType {
    @Json(name = "MULTISIG") MULTISIG,
    @Json(name = "MODULE") MODULE
}

sealed class DetailedExecutionInfo(
    @Json(name = "type") val type: DetailedExecutionInfoType
) {
    @JsonClass(generateAdapter = true)
    data class MultisigExecutionDetails(
        @Json(name = "submittedAt")
        val submittedAt: Date,
        @Json(name = "nonce")
        val nonce: BigInteger,
        @Json(name = "safeTxHash")
        val safeTxHash: String,
        @Json(name = "signers")
        val signers: List<Solidity.Address>,
        @Json(name = "confirmationsRequired")
        val confirmationsRequired: Int,
        @Json(name = "confirmations")
        val confirmations: List<Confirmations>,
        @Json(name = "executor")
        val executor: Solidity.Address?,
        @Json(name = "safeTxGas")
        val safeTxGas: BigInteger,
        @Json(name = "baseGas")
        val baseGas: BigInteger,
        @Json(name = "gasPrice")
        val gasPrice: BigInteger,
        @Json(name = "gasToken")
        val gasToken: Solidity.Address
    ) : DetailedExecutionInfo(DetailedExecutionInfoType.MULTISIG)

    @JsonClass(generateAdapter = true)
    data class ModuleExecutionDetails(
        @Json(name = "address")
        val address: String
    ) : DetailedExecutionInfo(DetailedExecutionInfoType.MODULE)
}

@JsonClass(generateAdapter = true)
data class Confirmations(
    @Json(name = "signer")
    val signer: Solidity.Address,
    @Json(name = "signature")
    val signature: String,
    @Json(name = "submittedAt")
    val submittedAt: Date
)
