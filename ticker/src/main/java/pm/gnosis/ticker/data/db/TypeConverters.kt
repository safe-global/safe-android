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

class FiatSymbolConverter {
    @TypeConverter
    fun fromString(string: String) = when (string) {
        "AUD" -> Currency.FiatSymbol.AUD
        "CAD" -> Currency.FiatSymbol.CAD
        "EUR" -> Currency.FiatSymbol.EUR
        "JPY" -> Currency.FiatSymbol.JPY
        "USD" -> Currency.FiatSymbol.USD
        "GBP" -> Currency.FiatSymbol.GBP
        else -> null
    }

    @TypeConverter
    fun toString(fiatSymbol: Currency.FiatSymbol) = fiatSymbol.symbol
}
