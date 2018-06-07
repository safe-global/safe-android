package pm.gnosis.heimdall.data.remote.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import java.math.BigInteger

@JsonClass(generateAdapter = true)
data class GnosisSafeTransactionDescription(
    @Json(name = "safeAddress")
    val safeAddress: Solidity.Address,
    @Json(name = "to")
    val to: Solidity.Address,
    @Json(name = "value")
    val value: Wei,
    @Json(name = "data")
    val data: String,
    @Json(name = "operation")
    val operation: BigInteger,
    @Json(name = "nonce")
    val nonce: BigInteger,
    @Json(name = "submittedAt")
    val submittedAt: Long,
    @Json(name = "subject")
    val subject: String?,
    @Json(name = "transactionHash")
    val transactionHash: String
)
