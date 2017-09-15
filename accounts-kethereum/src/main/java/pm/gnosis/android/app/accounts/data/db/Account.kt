package pm.gnosis.android.app.accounts.data.db

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = Account.TABLE_NAME)
class Account {
    @PrimaryKey
    @ColumnInfo(name = PRIVATE_KEY_COL)
    var privateKey: String? = null

    @ColumnInfo(name = MNEMONIC_COL)
    var verified: String? = null

    companion object {
        const val TABLE_NAME = "account"
        const val PRIVATE_KEY_COL = "private_key"
        const val MNEMONIC_COL = "mnemonic"
    }
}