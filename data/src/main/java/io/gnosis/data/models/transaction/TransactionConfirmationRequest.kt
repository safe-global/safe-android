package io.gnosis.data.models.transaction

import com.squareup.moshi.Json

data class TransactionConfirmationRequest(
    @Json(name = "signedSafeTxHash")
    val signedSafeTxHash: String
)
