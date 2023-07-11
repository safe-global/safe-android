package io.gnosis.data.models.transaction

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.gnosis.data.models.AddressInfo
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger
import java.util.*

@JsonClass(generateAdapter = true)
data class TransactionDetails(
    @Json(name = "txHash")
    val txHash: String? = null,
    @Json(name = "txStatus")
    val txStatus: TransactionStatus = TransactionStatus.PENDING,
    @Json(name = "txInfo")
    val txInfo: TransactionInfo,
    @Json(name = "executedAt")
    val executedAt: Date? = null,
    @Json(name = "txData")
    val txData: TxData? = null,
    @Json(name = "detailedExecutionInfo")
    val detailedExecutionInfo: DetailedExecutionInfo? = null,
    @Json(name = "safeAppInfo")
    val safeAppInfo: SafeAppInfo?
)


@JsonClass(generateAdapter = true)
data class TxData(
    @Json(name = "hexData")
    val hexData: String?,
    @Json(name = "dataDecoded")
    val dataDecoded: DataDecoded?,
    @Json(name = "to")
    val to: AddressInfo,
    @Json(name = "value")
    val value: BigInteger?,
    @Json(name = "operation")
    val operation: Operation,

    //FIXME: suboptimal backend response structure
    @Json(name = "addressInfoIndex")
    val addressInfoIndex: Map<String, AddressInfo>? = null
)


enum class DetailedExecutionInfoType {
    @Json(name = "MULTISIG")
    MULTISIG,
    @Json(name = "MODULE")
    MODULE
}


sealed class DetailedExecutionInfo(
    @Json(name = "type") val type: DetailedExecutionInfoType
) {
    @JsonClass(generateAdapter = true)
    data class MultisigExecutionDetails(
        @Json(name = "submittedAt")
        val submittedAt: Date = Date(),
        @Json(name = "nonce")
        val nonce: BigInteger,
        @Json(name = "safeTxHash")
        val safeTxHash: String = "",
        @Json(name = "signers")
        val signers: List<AddressInfo> = emptyList(),
        @Json(name = "rejectors")
        val rejectors: List<AddressInfo>? = emptyList(),
        @Json(name = "confirmationsRequired")
        val confirmationsRequired: Int = 0,
        @Json(name = "confirmations")
        val confirmations: List<Confirmations> = emptyList(),
        @Json(name = "executor")
        val executor: AddressInfo? = null,
        @Json(name = "refundReceiver")
        val refundReceiver: AddressInfo? = null,
        @Json(name = "safeTxGas")
        val safeTxGas: BigInteger = BigInteger.ZERO,
        @Json(name = "baseGas")
        val baseGas: BigInteger = BigInteger.ZERO,
        @Json(name = "gasPrice")
        val gasPrice: BigInteger = BigInteger.ZERO,
        @Json(name = "gasToken")
        val gasToken: Solidity.Address = "0".asEthereumAddress()!!
    ) : DetailedExecutionInfo(DetailedExecutionInfoType.MULTISIG)

    @JsonClass(generateAdapter = true)
    data class ModuleExecutionDetails(
        @Json(name = "address")
        val address: AddressInfo
    ) : DetailedExecutionInfo(DetailedExecutionInfoType.MODULE)
}

@JsonClass(generateAdapter = true)
data class Confirmations(
    @Json(name = "signer")
    val signer: AddressInfo,
    @Json(name = "signature")
    val signature: String,
    @Json(name = "submittedAt")
    val submittedAt: Date
)
