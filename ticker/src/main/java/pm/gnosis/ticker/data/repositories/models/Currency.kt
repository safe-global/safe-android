package pm.gnosis.ticker.data.repositories.models

import pm.gnosis.ticker.data.db.models.CurrencyDb
import pm.gnosis.ticker.data.remote.TickerApi
import pm.gnosis.ticker.data.remote.models.CurrencyNetwork
import pm.gnosis.utils.nullOnThrow
import java.math.BigDecimal

data class Currency(val id: String,
                    val name: String,
                    val symbol: String,
                    val rank: Long,
                    val lastUpdated: Long,
                    val price: BigDecimal,
                    val currency: FiatTicker) {
    fun getFiatSymbol(): String = nullOnThrow { java.util.Currency.getInstance(currency.ticker).symbol } ?: currency.ticker

    enum class FiatTicker(val ticker: String) {
        AUD("AUD"),
        CAD("CAD"),
        EUR("EUR"),
        JPY("JPY"),
        USD("USD"),
        GBP("GBP"),
    }
}

fun CurrencyDb.fromDb() = Currency(id, name, symbol, rank, lastUpdated, price, currency)
fun Currency.toDb() = CurrencyDb(id, name, symbol, rank, lastUpdated, price, currency)
fun CurrencyNetwork.fromNetwork(currency: Currency.FiatTicker) = Currency(
        id, name, symbol, rank.toLong(), lastUpdated,
        if (currency == Currency.FiatTicker.USD) BigDecimal(priceUsd) else BigDecimal(price),
        currency)
