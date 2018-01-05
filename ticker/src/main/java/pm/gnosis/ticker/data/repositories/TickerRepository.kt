package pm.gnosis.ticker.data.repositories

import io.reactivex.Single
import pm.gnosis.models.Wei
import pm.gnosis.ticker.data.repositories.models.Currency
import java.math.BigDecimal

interface TickerRepository {
    fun convertToFiat(amount: Wei, currency: Currency.FiatSymbol = Currency.FiatSymbol.USD): Single<Pair<BigDecimal, Currency>>
    fun convertToFiat(amounts: List<Wei>, currency: Currency.FiatSymbol = Currency.FiatSymbol.USD): Single<Pair<List<BigDecimal>, Currency>>
    fun loadCurrency(currency: Currency.FiatSymbol = Currency.FiatSymbol.USD): Single<Currency>
}
