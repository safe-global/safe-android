package pm.gnosis.ticker.data.repositories.impls

import io.reactivex.Single
import pm.gnosis.models.Wei
import pm.gnosis.ticker.data.db.TickerDatabase
import pm.gnosis.ticker.data.remote.TickerApi
import pm.gnosis.ticker.data.repositories.TickerRepository
import pm.gnosis.ticker.data.repositories.models.Currency
import pm.gnosis.ticker.data.repositories.models.fromDb
import pm.gnosis.ticker.data.repositories.models.fromNetwork
import pm.gnosis.ticker.data.repositories.models.toDb
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultTickerRepository @Inject constructor(
        private val tickerApi: TickerApi,
        private val tickerDb: TickerDatabase
) : TickerRepository {
    override fun convertToFiat(amount: Wei, currency: Currency.FiatSymbol): Single<Pair<BigDecimal, Currency>> =
            loadCurrency(currency)
                    .map { convert(amount, it) to it }

    override fun convertToFiat(amounts: List<Wei>, currency: Currency.FiatSymbol): Single<Pair<List<BigDecimal>, Currency>> =
            loadCurrency(currency).map { amounts.map { wei -> convert(wei, it) } to it }

    private fun convert(amount: Wei, currency: Currency) =
            (amount.toEther() * currency.price).setScale(3, RoundingMode.HALF_UP)

    override fun loadCurrency(currency: Currency.FiatSymbol): Single<Currency> =
            tickerApi.currency(currency.symbol)
                    .map { it.first().fromNetwork(currency) }
                    .doOnSuccess { tickerDb.tickerDao().setCurrency(it.toDb()) }
                    .onErrorResumeNext { tickerDb.tickerDao().loadCurrency().map { it.fromDb() } }
}
