package pm.gnosis.heimdall.data.remote.models

import com.squareup.moshi.Json
import pm.gnosis.heimdall.data.remote.models.push.ServiceSignature


data class ExecuteParams(
    @Json(name = "safe")
    val safe: String,
    @Json(name = "to")
    val to: String,
    @Json(name = "value")
    val value: String,
    @Json(name = "data")
    val data: String,
    @Json(name = "operation")
    val operation: Int,
    @Json(name = "signatures")
    val signatures: List<ServiceSignature>,
    @Json(name = "safeTxGas")
    val safeTxGas: String,
    @Json(name = "dataGas")
    val dataGas: String,
    @Json(name = "gasPrice")
    val gasPrice: String
)

data class RelayExecution(
    @Json(name = "transactionHash")
    val transactionHash: String
)


data class EstimateParams(
    @Json(name = "safe")
    val safe: String,
    @Json(name = "to")
    val to: String,
    @Json(name = "value")
    val value: String,
    @Json(name = "data")
    val data: String,
    @Json(name = "operation")
    val operation: Int,
    @Json(name = "threshold")
    val threshold: Int

)

data class RelayEstimate(
    @Json(name = "safeTxGas")
    val safeTxGas: String,
    @Json(name = "dataGas")
    val dataGas: String,
    @Json(name = "gasPrice")
    val gasPrice: String
)
