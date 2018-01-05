package pm.gnosis.ticker.data.remote

import io.reactivex.Single
import pm.gnosis.ticker.data.remote.models.CurrencyNetwork
import pm.gnosis.ticker.data.repositories.models.Currency
import retrofit2.http.GET
import retrofit2.http.Query

interface TickerApi {
    companion object {
        const val BASE_URL: String = "https://api.coinmarketcap.com/v1/ticker/"
    }

    @GET("ethereum")
    fun currency(@Query(value = "convert") ticker: String = Currency.FiatSymbol.USD.symbol): Single<List<CurrencyNetwork>>
}
