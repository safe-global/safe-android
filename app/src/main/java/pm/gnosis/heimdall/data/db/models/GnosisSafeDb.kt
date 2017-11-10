package pm.gnosis.heimdall.data.db.models

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import pm.gnosis.heimdall.data.repositories.models.Safe
import java.math.BigInteger

@Entity(tableName = GnosisSafeDb.TABLE_NAME)
data class GnosisSafeDb(
        @PrimaryKey
        @ColumnInfo(name = COL_ADDRESS)
        var address: BigInteger,

        @ColumnInfo(name = COL_NAME)
        var name: String?
) {
    companion object {
        const val TABLE_NAME = "gnosis_safes"
        const val COL_ADDRESS = "address"
        const val COL_NAME = "name"
    }
}

fun Safe.toDb() = GnosisSafeDb(address, name)
fun GnosisSafeDb.fromDb() = Safe(address, name)
