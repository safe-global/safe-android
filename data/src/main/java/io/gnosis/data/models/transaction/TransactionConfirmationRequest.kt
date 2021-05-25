package io.gnosis.data.models.transaction

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.gnosis.data.utils.ExcludeClassFromJacocoGeneratedReport

@ExcludeClassFromJacocoGeneratedReport
@JsonClass(generateAdapter = true)
data class TransactionConfirmationRequest(
    @Json(name = "signedSafeTxHash")
    val signedSafeTxHash: String
)
