package pm.gnosis.android.app.wallet.data.model

import com.squareup.moshi.Json

data class TransactionCallParams(@Json(name = "from") val from: String? = null,
                                 @Json(name = "to") val to: String? = null,
                                 @Json(name = "gas") val gas: String? = null,
                                 @Json(name = "gasPrice") val gasPrice: String? = null,
                                 @Json(name = "value") val value: String? = null,
                                 @Json(name = "data") val data: String? = null)
