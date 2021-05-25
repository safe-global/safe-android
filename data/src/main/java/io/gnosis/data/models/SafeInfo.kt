package io.gnosis.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.gnosis.data.utils.ExcludeClassFromJacocoGeneratedReport
import java.math.BigInteger

@ExcludeClassFromJacocoGeneratedReport
@JsonClass(generateAdapter = true)
data class SafeInfo(
    @Json(name = "address") val address: AddressInfoExtended,
    @Json(name = "nonce") val nonce: BigInteger,
    @Json(name = "threshold") val threshold: Int,
    @Json(name = "owners") val owners: List<AddressInfoExtended>,
    @Json(name = "implementation") val implementation: AddressInfoExtended,
    @Json(name = "modules") val modules: List<AddressInfoExtended>,
    @Json(name = "fallbackHandler") val fallbackHandler: AddressInfoExtended?,
    @Json(name = "version") val version: String
)
