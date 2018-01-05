package pm.gnosis.ticker.data.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import pm.gnosis.ticker.data.db.models.CurrencyDb

@Database(entities = [CurrencyDb::class], version = 1)
@TypeConverters(BigDecimalConverter::class, FiatSymbolConverter::class)
abstract class TickerDatabase : RoomDatabase() {
    companion object {
        const val DB_NAME = "gnosis-ticker-db"
    }

    abstract fun tickerDao(): TickerDao
}
