package pm.gnosis.heimdall.accounts.data.db

import android.arch.persistence.room.Database
import pm.gnosis.heimdall.accounts.repositories.impl.models.db.AccountDb

@Database(entities = arrayOf(AccountDb::class), version = 1)
abstract class AccountsDatabase : RoomDatabase() {
    companion object {
        const val DB_NAME = "gnosis-accounts-db"
    }

    abstract fun accountsDao(): AccountDao
}
