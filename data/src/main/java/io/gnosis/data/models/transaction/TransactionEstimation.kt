package io.gnosis.data.models.transaction

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import pm.gnosis.common.adapters.moshi.DecimalNumber
import pm.gnosis.model.Solidity
import java.math.BigInteger

@JsonClass(generateAdapter = true)
data class TransactionEstimationRequest(
    @Json(name = "to")
    val to: Solidity.Address,

    @Json(name = "value")
    @field:DecimalNumber
    val value: BigInteger,

    @Json(name = "data")
    val data: String = "",

    @Json(name = "operation")
    val operation: Operation = Operation.CALL,

    @Json(name = "chainId")
    val chainId: BigInteger
)

@JsonClass(generateAdapter = true)
data class TransactionEstimation(
    @Json(name = "currentNonce")
    val currentNonce: BigInteger,

    @Json(name = "recommendedNonce")
    val recommendedNonce: BigInteger,

    @Json(name = "safeTxGas")
    val safeTxGas: BigInteger
)
