package pm.gnosis.ticker.data.remote

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import pm.gnosis.ticker.data.remote.models.CurrencyNetwork

class TickerAdapter {
    @FromJson
    fun fromJson(reader: JsonReader, delegate: JsonAdapter<Map<String, String>>): CurrencyNetwork {
        val map = delegate.fromJson(reader).mapKeys {
            if (it.key.startsWith("price_") && it.key != "price_usd" && it.key != "price_btc") {
                "price"
            } else {
                it.key
            }
        }
        return CurrencyNetwork(
            map["id"]!!,
            map["name"]!!,
            map["symbol"]!!,
            map["rank"]!!,
            map["price_usd"]!!,
            map["price_btc"]!!,
            map["last_updated"]!!.toLong(),
            map["price"]
        )
    }
}
