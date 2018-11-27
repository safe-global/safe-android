package pm.gnosis.heimdall.data.remote.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import pm.gnosis.heimdall.data.adapters.DecimalNumber
import pm.gnosis.heimdall.data.remote.models.push.ServiceSignature
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import java.math.BigInteger

@JsonClass(generateAdapter = true)
data class ExecuteParams(
    @Json(name = "to")
    val to: String,
    @Json(name = "value")
    val value: String,
    @Json(name = "data")
    val data: String?,
    @Json(name = "operation")
    val operation: Int,
    @Json(name = "signatures")
    val signatures: List<ServiceSignature>,
    @Json(name = "safeTxGas")
    val safeTxGas: String,
    @Json(name = "dataGas")
    val dataGas: String,
    @Json(name = "gasPrice")
    val gasPrice: String,
    @Json(name = "gasToken")
    val gasToken: String,
    @Json(name = "nonce")
    val nonce: Long
)

@JsonClass(generateAdapter = true)
data class RelayExecution(
    @Json(name = "transactionHash")
    val transactionHash: String
)

@JsonClass(generateAdapter = true)
data class EstimateParams(
    @Json(name = "to")
    val to: String,
    @Json(name = "value")
    val value: String,
    @Json(name = "data")
    val data: String,
    @Json(name = "operation")
    val operation: Int,
    @Json(name = "threshold")
    val threshold: Int,
    @Json(name = "gasToken")
    val gasToken: Solidity.Address
)

@JsonClass(generateAdapter = true)
data class RelayEstimate(
    @Json(name = "safeTxGas")
    val safeTxGas: String,
    @Json(name = "dataGas")
    val dataGas: String,
    @Json(name = "operationalGas")
    val operationalGas: String,
    @Json(name = "gasPrice")
    val gasPrice: String,
    @Json(name = "gasToken")
    val gasToken: Solidity.Address,
    @Json(name = "lastUsedNonce")
    val lastUsedNonce: String?
)

@JsonClass(generateAdapter = true)
data class RelaySafeCreationParams(
    @Json(name = "owners") val owners: List<Solidity.Address>,
    @Json(name = "threshold") val threshold: Int,
    @Json(name = "s") @field:DecimalNumber val s: BigInteger,
    @Json(name = "paymentToken") val paymentToken: Solidity.Address
)

@JsonClass(generateAdapter = true)
data class RelaySafeCreation(
    @Json(name = "signature") val signature: ServiceSignature,
    @Json(name = "tx") val tx: RelaySafeCreationTx,
    @Json(name = "safe") val safe: Solidity.Address,
    @Json(name = "payment") @field:DecimalNumber val payment: BigInteger,
    @Json(name = "paymentToken") val paymentToken: Solidity.Address?,
    @Json(name = "funder") val funder: Solidity.Address?
)

@JsonClass(generateAdapter = true)
data class RelaySafeCreationTx(
    @Json(name = "from") val from: Solidity.Address,
    @Json(name = "value") val value: Wei,
    @Json(name = "data") val data: String,
    @Json(name = "gas") @field:DecimalNumber val gas: BigInteger,
    @Json(name = "gasPrice") @field:DecimalNumber val gasPrice: BigInteger,
    @Json(name = "nonce") @field:DecimalNumber val nonce: BigInteger
)

@JsonClass(generateAdapter = true)
data class RelaySafeFundStatus(
    @Json(name = "safeFunded") val safeFunded: Boolean,
    @Json(name = "deployerFunded") val deployerFunded: Boolean,
    @Json(name = "deployerFundedTxHash") val deployerFundedTxHash: String?,
    @Json(name = "safeDeployed") val safeDeployed: Boolean,
    @Json(name = "safeDeployedTxHash") val safeDeployedTxHash: String?
)
