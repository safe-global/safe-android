package io.gnosis.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigInteger

@JsonClass(generateAdapter = true)
data class SafeInfo(
    @Json(name = "address") val address: AddressInfo,
    @Json(name = "nonce") val nonce: BigInteger,
    @Json(name = "threshold") val threshold: Int,
    @Json(name = "owners") val owners: List<AddressInfo>,
    @Json(name = "implementation") val implementation: AddressInfo,
    @Json(name = "modules") val modules: List<AddressInfo>?,
    @Json(name = "fallbackHandler") val fallbackHandler: AddressInfo?,
    @Json(name = "version") val version: String
)
