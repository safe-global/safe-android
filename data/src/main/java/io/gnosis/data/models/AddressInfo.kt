package io.gnosis.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import pm.gnosis.model.Solidity

@JsonClass(generateAdapter = true)
data class AddressInfo(
    @Json(name = "value") val value: Solidity.Address,
    @Json(name = "name") val name: String? = null,
    @Json(name = "logoUri") val logoUri: String? = null
)
