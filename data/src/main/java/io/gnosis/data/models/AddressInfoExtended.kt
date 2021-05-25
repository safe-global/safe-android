package io.gnosis.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.gnosis.data.utils.ExcludeClassFromJacocoGeneratedReport
import pm.gnosis.model.Solidity

@ExcludeClassFromJacocoGeneratedReport
@JsonClass(generateAdapter = true)
data class AddressInfoExtended(
    @Json(name = "value") val value: Solidity.Address,
    @Json(name = "name") val name: String? = null,
    @Json(name = "logoUrl") val logoUrl: String? = null
)
