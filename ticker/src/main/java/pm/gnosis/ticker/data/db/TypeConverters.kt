package pm.gnosis.ticker.data.db

import android.arch.persistence.room.TypeConverter
import pm.gnosis.ticker.data.repositories.models.Currency
import pm.gnosis.utils.nullOnThrow
import java.math.BigDecimal

class BigDecimalConverter {
    @TypeConverter
    fun fromString(string: String?) = nullOnThrow { BigDecimal(string) }

    @TypeConverter
    fun toString(value: BigDecimal?) = nullOnThrow { value.toString() }
}

class CurrencyFiatTickerConverter {
    @TypeConverter
    fun fromString(string: String) = when (string) {
        "AUD" -> Currency.FiatTicker.AUD
        "CAD" -> Currency.FiatTicker.CAD
        "EUR" -> Currency.FiatTicker.EUR
        "JPY" -> Currency.FiatTicker.JPY
        "USD" -> Currency.FiatTicker.USD
        "GBP" -> Currency.FiatTicker.GBP
        else -> null
    }

    @TypeConverter
    fun toString(fiatTicker: Currency.FiatTicker) = fiatTicker.ticker
}
