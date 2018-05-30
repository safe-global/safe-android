package pm.gnosis.heimdall.data.db.models

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import java.math.BigInteger

@Entity(tableName = GnosisSafeDb.TABLE_NAME)
data class GnosisSafeDb(
    @PrimaryKey
    @ColumnInfo(name = COL_ADDRESS)
    var address: Solidity.Address,

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

@Entity(tableName = PendingGnosisSafeDb.TABLE_NAME)
data class PendingGnosisSafeDb(
    @PrimaryKey
    @ColumnInfo(name = COL_TX_HASH)
    var transactionHash: BigInteger,

    @ColumnInfo(name = COL_NAME)
    var name: String?,

    @ColumnInfo(name = COL_SAFE_ADDRESS)
    var address: Solidity.Address,

    @ColumnInfo(name = COL_PAYMENT)
    var payment: Wei,

    @ColumnInfo(name = COL_IS_FUNDED)
    var isFunded: Boolean = false
) {
    companion object {
        const val TABLE_NAME = "gnosis_pending_safes"
        const val COL_TX_HASH = "transactionHash"
        const val COL_NAME = "name"
        const val COL_SAFE_ADDRESS = "address"
        const val COL_PAYMENT = "payment"
        const val COL_IS_FUNDED = "funded"
    }
}

fun PendingSafe.toDb() = PendingGnosisSafeDb(hash, name, address, payment, isFunded)
fun PendingGnosisSafeDb.fromDb() = PendingSafe(transactionHash, name, address, payment, isFunded)
