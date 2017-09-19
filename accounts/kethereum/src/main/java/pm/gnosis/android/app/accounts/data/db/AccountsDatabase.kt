package pm.gnosis.android.app.accounts.data.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import pm.gnosis.android.app.accounts.repositories.impl.models.db.AccountDb

@Database(entities = arrayOf(AccountDb::class), version = 1)
abstract class AccountsDatabase : RoomDatabase() {
    companion object {
        const val DB_NAME = "gnosis-accounts-db"
    }

    abstract fun accountsDao(): AccountDao
}