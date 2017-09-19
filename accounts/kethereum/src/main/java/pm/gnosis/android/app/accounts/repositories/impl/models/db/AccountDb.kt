package pm.gnosis.android.app.accounts.repositories.impl.models.db

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = AccountDb.TABLE_NAME)
class AccountDb {
    @PrimaryKey
    @ColumnInfo(name = PRIVATE_KEY_COL)
    var privateKey: String? = null

    companion object {
        const val TABLE_NAME = "account"
        const val PRIVATE_KEY_COL = "private_key"
    }
}
