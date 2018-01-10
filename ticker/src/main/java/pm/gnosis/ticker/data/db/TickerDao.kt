package pm.gnosis.ticker.data.db

import android.arch.persistence.room.*
import io.reactivex.Single
import pm.gnosis.ticker.data.db.models.CurrencyDb

@Dao
abstract class TickerDao {
    @Transaction
    open fun setCurrency(currency: CurrencyDb) {
        clearCurrencies()
        insertCurrency(currency)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertCurrency(currency: CurrencyDb)

    @Query("DELETE FROM ${CurrencyDb.TABLE_NAME}")
    abstract fun clearCurrencies()

    @Query("SELECT * FROM ${CurrencyDb.TABLE_NAME}")
    abstract fun loadCurrency(): Single<CurrencyDb>
}
