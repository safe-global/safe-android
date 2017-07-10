package pm.gnosis.android.app.wallet.data.model

import com.squareup.moshi.Json

data class Balance(@Json(name = "status") val status: String,
                   @Json(name = "message") val message: String,
                   @Json(name = "result") val result: Wei)
