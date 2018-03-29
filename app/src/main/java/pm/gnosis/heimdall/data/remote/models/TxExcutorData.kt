package pm.gnosis.heimdall.data.remote.models

import com.squareup.moshi.Json

data class TxExecutionData(
    @Json(name = "target")
    val target: String,
    @Json(name = "data")
    val data: String?
)

data class TxExecutionResponse(
    @Json(name = "hash")
    val hash: String
)

data class TxExecutionBalance(
    @Json(name = "balance")
    val balance: Long
)

data class TxExecutionEstimate(
    @Json(name = "balance")
    val balance: Long,
    @Json(name = "required_credits")
    val requiredCredits: Long
)

data class TxExecutionVoucherData(
    @Json(name = "voucher_id")
    val voucherId: String
)
