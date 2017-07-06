package pm.gnosis.android.app.wallet.data.model

import com.squareup.moshi.Json

data class Transaction(
        @Json(name = "nonce") val nonce: String,
        @Json(name = "to") val to: String,
        @Json(name = "data") val data: String,
        @Json(name = "value") val value: String,
        @Json(name = "gasLimit") val gasLimit: String,
        @Json(name = "gasPrice") val gasPrice: String,
        @Json(name = "v") val v: String,
        @Json(name = "r") val r: String,
        @Json(name = "s") val s: String)
