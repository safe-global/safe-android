package pm.gnosis.ticker.data.repositories.models

import pm.gnosis.models.Wei
import pm.gnosis.ticker.data.db.models.CurrencyDb
import pm.gnosis.ticker.data.remote.models.CurrencyNetwork
import pm.gnosis.utils.nullOnThrow
import java.math.BigDecimal
import java.math.RoundingMode

data class Currency(
    val id: String,
    val name: String,
    val symbol: String,
    val rank: Long,
    val lastUpdated: Long,
    val price: BigDecimal,
    val fiatSymbol: FiatSymbol
) {
    fun getFiatSymbol(): String = nullOnThrow { java.util.Currency.getInstance(fiatSymbol.symbol).symbol } ?: fiatSymbol.symbol

    fun convert(crypto: Wei): BigDecimal =
        (crypto.toEther() * price).setScale(3, RoundingMode.HALF_UP)

    enum class FiatSymbol(val symbol: String) {
        AUD("AUD"),
        CAD("CAD"),
        EUR("EUR"),
        JPY("JPY"),
        USD("USD"),
        GBP("GBP"),
    }
}

fun CurrencyDb.fromDb() = Currency(id, name, symbol, rank, lastUpdated, price, fiatSymbol)
fun Currency.toDb() = CurrencyDb(id, name, symbol, rank, lastUpdated, price, fiatSymbol)
fun CurrencyNetwork.fromNetwork(fiatSymbol: Currency.FiatSymbol) = Currency(
    id, name, symbol, rank.toLong(), lastUpdated,
    if (fiatSymbol == Currency.FiatSymbol.USD) BigDecimal(priceUsd) else BigDecimal(price),
    fiatSymbol
)
