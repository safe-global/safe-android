package io.gnosis.data.backend.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import pm.gnosis.model.Solidity
import java.math.BigInteger

@JsonClass(generateAdapter = true)
data class SafeInfoDto(
    @Json(name = "address") val address: Solidity.Address,
    @Json(name = "nonce") val nonce: BigInteger,
    @Json(name = "threshold") val threshold: Int,
    @Json(name = "owners") val owners: List<Solidity.Address>,
    @Json(name = "masterCopy") val masterCopy: Solidity.Address,
    @Json(name = "modules") val modules: List<Solidity.Address>,
    @Json(name = "fallbackHandler") val fallbackHandler: Solidity.Address,
    @Json(name = "version") val version: String
)
