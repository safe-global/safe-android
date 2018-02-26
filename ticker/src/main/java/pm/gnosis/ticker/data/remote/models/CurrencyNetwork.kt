package pm.gnosis.ticker.data.remote.models

import com.squareup.moshi.Json

data class CurrencyNetwork(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "rank") val rank: String,
    @Json(name = "price_usd") val priceUsd: String,
    @Json(name = "price_btc") val priceBtc: String,
    @Json(name = "last_updated") val lastUpdated: Long,
    @Json(name = "price") val price: String?
)
