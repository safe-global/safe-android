package pm.gnosis.ticker.data.db.models

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import pm.gnosis.ticker.data.repositories.models.Currency
import java.math.BigDecimal

@Entity(tableName = CurrencyDb.TABLE_NAME)
data class CurrencyDb(
        @PrimaryKey
        @ColumnInfo(name = ID_COL)
        var id: String,

        @ColumnInfo(name = NAME_COL)
        var name: String,

        @ColumnInfo(name = SYMBOL_COL)
        val symbol: String,

        @ColumnInfo(name = RANK_COL)
        val rank: Long,

        @ColumnInfo(name = LAST_UPDATED_COL)
        val lastUpdated: Long,

        @ColumnInfo(name = PRICE_COL)
        val price: BigDecimal,

        @ColumnInfo(name = CURRENCY_COL)
        val currency: Currency.FiatTicker
) {
    companion object {
        const val TABLE_NAME = "ticker_currency"
        const val ID_COL = "id"
        const val NAME_COL = "name"
        const val SYMBOL_COL = "symbol"
        const val RANK_COL = "rank"
        const val LAST_UPDATED_COL = "last_updated"
        const val PRICE_COL = "price"
        const val CURRENCY_COL = "currency"
    }
}
