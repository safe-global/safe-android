package pm.gnosis.heimdall.data.remote.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
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

@JsonClass(generateAdapter = true)
data class TxExecutionBalance(
    @Json(name = "balance")
    val balance: Long
)

@JsonClass(generateAdapter = true)
data class TxExecutionEstimate(
    @Json(name = "balance")
    val balance: Long,
    @Json(name = "required_credits")
    val requiredCredits: Long
)

@JsonClass(generateAdapter = true)
data class TxExecutionVoucherData(
    @Json(name = "voucher_id")
    val voucherId: String
)
