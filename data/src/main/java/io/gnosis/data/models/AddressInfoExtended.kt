package io.gnosis.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import pm.gnosis.model.Solidity

@JsonClass(generateAdapter = true)
data class AddressInfoExtended(
    @Json(name = "value") val value: Solidity.Address,
    @Json(name = "name") val name: String?,
    @Json(name = "logoUri") val logoUri: String?
)
