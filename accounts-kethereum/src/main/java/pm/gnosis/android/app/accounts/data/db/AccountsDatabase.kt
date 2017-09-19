package pm.gnosis.android.app.accounts.data.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database(entities = arrayOf(Account::class), version = 1)
abstract class AccountsDatabase : RoomDatabase() {
    companion object {
        const val DB_NAME = "gnosis-accounts-db"
    }

    abstract fun accountsDao(): AccountDao
}