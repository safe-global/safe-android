package io.gnosis.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.gnosis.data.utils.ExcludeClassFromJacocoGeneratedReport

@ExcludeClassFromJacocoGeneratedReport
@JsonClass(generateAdapter = true)
data class AddressInfo(
    @Json(name = "name") val name: String,
    @Json(name = "logoUri") val logoUri: String?
)
