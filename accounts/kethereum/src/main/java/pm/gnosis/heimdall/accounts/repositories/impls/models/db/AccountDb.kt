package pm.gnosis.heimdall.accounts.repositories.impls.models.db

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import pm.gnosis.heimdall.security.db.EncryptedByteArray

@Entity(tableName = AccountDb.TABLE_NAME)
data class AccountDb(
        @PrimaryKey
        @ColumnInfo(name = PRIVATE_KEY_COL)
        var privateKey: EncryptedByteArray
) {
    companion object {
        const val TABLE_NAME = "account"
        const val PRIVATE_KEY_COL = "private_key"
    }
}
