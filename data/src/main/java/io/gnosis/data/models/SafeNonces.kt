package io.gnosis.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigInteger

@JsonClass(generateAdapter = true)
data class SafeNonces(
    @Json(name = "currentNonce")
    val currentNonce: BigInteger,

    @Json(name = "recommendedNonce")
    val recommendedNonce: BigInteger
)
